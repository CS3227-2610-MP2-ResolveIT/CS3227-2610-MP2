package resolveit.frontend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;

class AuthServiceTest {
    @Test
    void trimsEmailAndStoresSuccessfulSession() {
        var session = new SessionState();
        var user = new User(1, "manager", "manager@resolveit.local",
                Role.MANAGER, true, null, null);
        var response = new LoginResponse("token", "Bearer", 900, user);
        AuthClient client = request -> {
            assertEquals("manager@resolveit.local", request.email());
            return CompletableFuture.completedFuture(response);
        };

        new AuthService(client, session)
                .login("  manager@resolveit.local ", "password")
                .toCompletableFuture().join();

        assertEquals(user, session.current().orElseThrow().user());
    }
}
