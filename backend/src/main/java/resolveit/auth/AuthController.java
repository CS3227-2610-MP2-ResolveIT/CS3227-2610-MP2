package resolveit.auth;

import static resolveit.auth.AuthDtos.LoginRequest;
import static resolveit.auth.AuthDtos.LoginResponse;
import static resolveit.auth.AuthDtos.LogoutRequest;
import static resolveit.auth.AuthDtos.RefreshRequest;
import static resolveit.auth.AuthDtos.RefreshResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import resolveit.common.ApiException;
import resolveit.user.UserDtos.UserResponse;
import resolveit.user.UserRepository;

/** Exposes login, refresh, logout, and current-user authentication endpoints. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository users;

    /**
     * Creates the authentication controller.
     *
     * @param authService authentication lifecycle service
     * @param users user persistence used by the current-user endpoint
     */
    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    /**
     * Authenticates submitted credentials.
     *
     * @param request submitted credentials
     * @return authenticated user and initial token pair
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Exchanges a usable refresh credential for a rotated token pair.
     *
     * @param request refresh credential to exchange
     * @return replacement access and refresh credentials
     */
    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    /**
     * Revokes the presented refresh credential for the authenticated user.
     *
     * @param request refresh credential to revoke
     * @param authentication authenticated account identity
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, Authentication authentication) {
        authService.logout(request, authentication.getName());
    }

    /**
     * Returns the active authenticated user.
     *
     * @param authentication authenticated account identity
     * @return current user details
     */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
    }
}
