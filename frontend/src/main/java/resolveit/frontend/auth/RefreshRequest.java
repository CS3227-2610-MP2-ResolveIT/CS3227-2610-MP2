package resolveit.frontend.auth;

/**
 * Request body used to exchange a refresh credential.
 *
 * @param refreshToken opaque refresh credential to exchange
 */
public record RefreshRequest(String refreshToken) {
}
