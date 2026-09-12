package resolveit.frontend.auth;

/**
 * Request body used to revoke a refresh credential.
 *
 * @param refreshToken opaque refresh credential to revoke
 */
public record LogoutRequest(String refreshToken) {
}
