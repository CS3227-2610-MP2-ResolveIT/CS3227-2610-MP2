package resolveit.frontend.auth;

import resolveit.frontend.model.User;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        User user) {
}
