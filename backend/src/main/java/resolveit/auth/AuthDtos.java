package resolveit.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import resolveit.user.UserDtos.UserResponse;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
}
