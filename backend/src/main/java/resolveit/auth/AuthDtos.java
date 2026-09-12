package resolveit.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import resolveit.user.UserDtos.UserResponse;

/** Authentication request and response contracts exposed by the REST API. */
public final class AuthDtos {
    private AuthDtos() {}

    /**
     * Credentials submitted to the login endpoint.
     *
     * @param email account email
     * @param password account password
     */
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /**
     * Successful login payload containing the user and initial token pair.
     *
     * @param accessToken short-lived bearer token
     * @param refreshToken opaque refresh credential
     * @param tokenType HTTP authorization scheme
     * @param expiresIn access-token lifetime in seconds
     * @param refreshExpiresIn refresh-token lifetime in seconds
     * @param user authenticated user
     */
    public record LoginResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresIn, long refreshExpiresIn, UserResponse user) {}

    /**
     * Request body for exchanging a refresh credential.
     *
     * @param refreshToken opaque refresh credential to exchange
     */
    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {}

    /**
     * Replacement credentials returned after refresh-token rotation.
     *
     * @param accessToken replacement bearer token
     * @param refreshToken rotated opaque refresh credential
     * @param tokenType HTTP authorization scheme
     * @param expiresIn access-token lifetime in seconds
     * @param refreshExpiresIn refresh-token lifetime in seconds
     */
    public record RefreshResponse(String accessToken, String refreshToken, String tokenType,
                                  long expiresIn, long refreshExpiresIn) {}

    /**
     * Request body for revoking a refresh credential.
     *
     * @param refreshToken opaque refresh credential to revoke
     */
    public record LogoutRequest(@NotBlank @Size(max = 512) String refreshToken) {}
}
