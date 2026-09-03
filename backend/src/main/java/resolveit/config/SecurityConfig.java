package resolveit.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import resolveit.user.UserRepository;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecret(@Value("${resolveit.jwt.secret}") String encodedSecret) {
        var decoded = Base64.getDecoder().decode(encodedSecret);
        if (decoded.length < 32) throw new IllegalStateException("JWT secret must contain at least 256 bits.");
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secret) {
        return NimbusJwtEncoder.withSecretKey(secret).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secret, UserRepository users,
                          @Value("${resolveit.jwt.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> activeUser = jwt -> users.findByEmailIgnoreCase(jwt.getSubject())
                .filter(user -> jwt.getClaim("uid") instanceof Number id
                        && user.isActive() && user.getId().intValue() == id.intValue())
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The user is inactive or no longer exists.", null)));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), activeUser));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/users/**", "/api/v1/technicians").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> users.findByEmailIgnoreCase(token.getSubject())
                                .map(user -> new JwtAuthenticationToken(token,
                                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                                        user.getEmail()))
                                .orElseGet(() -> new JwtAuthenticationToken(token))))
                        .authenticationEntryPoint((request, response, exception) -> writeError(response, 401,
                                "UNAUTHORIZED", "Authentication is required.")))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        writeError(response, 403, "ACCESS_DENIED", "You are not allowed to perform this action.")))
                .build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse response, int status,
                                   String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().printf("{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\"}", status, code, message);
    }
}
