package resolveit.frontend.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;

class SessionStateTest {
    @Test
    void startsAndClearsSession() {
        var state = new SessionState();
        var user = new User(4, "employee01", "employee01@resolveit.local",
                Role.EMPLOYEE, true, "2026-09-04T00:00:00Z", "2026-09-04T00:00:00Z");

        state.start(new LoginResponse("secret-token", "Bearer", 900, user));

        assertEquals(user, state.current().orElseThrow().user());
        assertEquals("secret-token", state.current().orElseThrow().accessToken());

        state.clear();
        assertTrue(state.current().isEmpty());
    }
}
