package resolveit.frontend.auth;

/**
 * Replacement access and refresh credentials returned after rotation.
 *
 * @param accessToken short-lived bearer token
 * @param refreshToken rotated opaque refresh credential
 * @param tokenType HTTP authorization scheme
 * @param expiresIn access-token lifetime in seconds
 * @param refreshExpiresIn refresh-token lifetime in seconds
 */
public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn) {
}
