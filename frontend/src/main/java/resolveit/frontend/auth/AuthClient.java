package resolveit.frontend.auth;

import java.util.concurrent.CompletionStage;

/** Defines the asynchronous authentication HTTP boundary. */
public interface AuthClient {
    /**
     * Authenticates credentials and obtains the initial token pair.
     *
     * @param request submitted login credentials
     * @return a stage containing the authenticated user and token pair
     */
    CompletionStage<LoginResponse> login(LoginRequest request);

    /**
     * Exchanges a refresh token for a rotated token pair.
     *
     * @param request refresh credential to exchange
     * @return a stage containing replacement credentials
     */
    CompletionStage<RefreshResponse> refresh(RefreshRequest request);

    /**
     * Requests server-side revocation of the supplied refresh token.
     *
     * @param request refresh credential to revoke
     * @param authorization access-token authorization header
     * @return a stage that completes after the server handles the request
     */
    CompletionStage<Void> logout(LogoutRequest request, String authorization);
}
