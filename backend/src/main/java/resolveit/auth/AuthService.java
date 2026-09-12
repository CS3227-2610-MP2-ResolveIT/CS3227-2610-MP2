package resolveit.auth;

import static resolveit.auth.AuthDtos.LoginRequest;
import static resolveit.auth.AuthDtos.LoginResponse;
import static resolveit.auth.AuthDtos.LogoutRequest;
import static resolveit.auth.AuthDtos.RefreshRequest;
import static resolveit.auth.AuthDtos.RefreshResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resolveit.common.ApiException;
import resolveit.user.User;
import resolveit.user.UserDtos.UserResponse;
import resolveit.user.UserRepository;

/** Authenticates users and manages access and refresh credential lifecycles. */
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenService refreshTokens;
    private final LoginAttemptLimiter loginAttempts;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenTtl;

    /**
     * Creates the authentication service from its security and persistence
     * collaborators.
     *
     * @param users user repository
     * @param passwordEncoder password verifier
     * @param jwtEncoder access-token encoder
     * @param refreshTokens refresh-token service
     * @param loginAttempts repeated-login limiter
     * @param clock authentication clock
     * @param issuer access-token issuer
     * @param accessTokenTtl access-token lifetime
     */
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       RefreshTokenService refreshTokens, LoginAttemptLimiter loginAttempts, Clock clock,
                       @Value("${resolveit.jwt.issuer}") String issuer,
                       @Value("${resolveit.jwt.access-token-ttl}") Duration accessTokenTtl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.loginAttempts = loginAttempts;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
    }

    /**
     * Authenticates credentials and issues the initial access and refresh pair.
     *
     * @param request submitted credentials
     * @param remoteAddress direct client address used by the login limiter
     * @return the authenticated user and issued credentials
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String remoteAddress) {
        var email = request.email().trim();
        loginAttempts.checkAllowed(email, remoteAddress);
        var user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isActive()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttempts.recordFailure(email, remoteAddress);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "The email or password is incorrect.");
        }
        loginAttempts.recordSuccess(email, remoteAddress);
        var now = clock.instant();
        var accessToken = issueAccessToken(user, now);
        var refreshToken = refreshTokens.issue(user, now);
        return new LoginResponse(accessToken.value(), refreshToken.value(), "Bearer",
                accessTokenTtl.toSeconds(), refreshTokens.ttl().toSeconds(), UserResponse.from(user));
    }

    /**
     * Exchanges one usable refresh token for a newly rotated token pair.
     *
     * @param request refresh credential to exchange
     * @return replacement access and refresh credentials
     */
    public RefreshResponse refresh(RefreshRequest request) {
        var rotated = refreshTokens.rotate(request.refreshToken());
        var accessToken = issueAccessToken(rotated.user(), clock.instant());
        return new RefreshResponse(accessToken.value(), rotated.value(), "Bearer",
                accessTokenTtl.toSeconds(), refreshTokens.ttl().toSeconds());
    }

    /**
     * Revokes the presented refresh token when it belongs to the authenticated
     * user.
     *
     * @param request refresh credential to revoke
     * @param authenticatedEmail authenticated account that must own the token
     */
    public void logout(LogoutRequest request, String authenticatedEmail) {
        refreshTokens.revoke(request.refreshToken(), authenticatedEmail);
    }

    private IssuedAccessToken issueAccessToken(User user, Instant now) {
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token);
    }

    private record IssuedAccessToken(String value) {}
}
