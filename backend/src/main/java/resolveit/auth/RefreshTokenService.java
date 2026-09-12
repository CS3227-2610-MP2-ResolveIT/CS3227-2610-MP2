package resolveit.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resolveit.common.ApiException;
import resolveit.user.User;

/** Issues opaque refresh credentials while persisting only their hashes. */
@Service
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;
    private static final String INVALID_CODE = "INVALID_REFRESH_TOKEN";
    private static final String INVALID_MESSAGE = "The session is invalid or has expired.";

    private final RefreshTokenRepository tokens;
    private final Clock clock;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a service for issuing, rotating, and revoking refresh tokens.
     *
     * @param tokens refresh-token persistence
     * @param clock authentication clock
     * @param ttl lifetime of newly issued refresh tokens
     */
    public RefreshTokenService(RefreshTokenRepository tokens, Clock clock,
                               @Value("${resolveit.jwt.refresh-token-ttl}") Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Refresh-token lifetime must be positive.");
        }
        this.tokens = tokens;
        this.clock = clock;
        this.ttl = ttl;
    }

    /**
     * Returns the configured lifetime of newly issued refresh tokens.
     *
     * @return configured refresh-token lifetime
     */
    public Duration ttl() {
        return ttl;
    }

    /**
     * Issues a new raw refresh token and stores only its hash.
     *
     * @param user active user that owns the token
     * @param now issuance time
     * @return raw token value and expiry for the response
     */
    @Transactional
    public IssuedToken issue(User user, Instant now) {
        tokens.deleteByExpiresAtBefore(now);
        return issueWithoutCleanup(user, now);
    }

    /**
     * Atomically consumes one refresh token and returns its replacement.
     *
     * @param rawToken refresh credential presented by the client
     * @return replacement token and its active owner
     */
    @Transactional
    public RotatedToken rotate(String rawToken) {
        var now = clock.instant();
        var tokenHash = hash(rawToken);
        var existing = tokens.findByTokenHash(tokenHash)
                .filter(token -> token.isUsableAt(now))
                .filter(token -> token.getUser().isActive())
                .orElseThrow(RefreshTokenService::invalidToken);
        var user = existing.getUser();

        if (tokens.revokeIfUsable(tokenHash, now) != 1) {
            throw invalidToken();
        }

        var replacement = issueWithoutCleanup(user, now);
        return new RotatedToken(user, replacement.value(), replacement.expiresAt());
    }

    /**
     * Revokes a token only when it belongs to the authenticated user.
     *
     * @param rawToken refresh credential to revoke
     * @param authenticatedEmail authenticated account that must own the token
     */
    @Transactional
    public void revoke(String rawToken, String authenticatedEmail) {
        tokens.revokeForUser(hash(rawToken), authenticatedEmail, clock.instant());
    }

    private IssuedToken issueWithoutCleanup(User user, Instant now) {
        var rawToken = generateToken();
        var expiresAt = now.plus(ttl);
        tokens.save(new RefreshToken(user, hash(rawToken), expiresAt, now));
        return new IssuedToken(rawToken, expiresAt);
    }

    private String generateToken() {
        var bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static ApiException invalidToken() {
        return ApiException.unauthorized(INVALID_CODE, INVALID_MESSAGE);
    }

    /**
     * Raw value and expiry returned only when a refresh token is issued.
     *
     * @param value raw opaque refresh credential
     * @param expiresAt credential expiry
     */
    public record IssuedToken(String value, Instant expiresAt) {}

    /**
     * Replacement token together with the active user that owns it.
     *
     * @param user active token owner
     * @param value raw replacement credential
     * @param expiresAt replacement credential expiry
     */
    public record RotatedToken(User user, String value, Instant expiresAt) {}
}
