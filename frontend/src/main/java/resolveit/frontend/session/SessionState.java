package resolveit.frontend.session;

import java.time.Instant;
import java.util.Optional;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.User;

public final class SessionState {
    private Session session;

    public synchronized void start(LoginResponse response) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                || response.user() == null) {
            throw new IllegalArgumentException("A complete login response is required.");
        }
        session = new Session(response.accessToken(), response.tokenType(),
                Instant.now().plusSeconds(response.expiresIn()), response.user());
    }

    public synchronized Optional<Session> current() {
        return Optional.ofNullable(session);
    }

    public synchronized void clear() {
        session = null;
    }

    public record Session(String accessToken, String tokenType, Instant expiresAt, User user) {
    }
}
