package resolveit.frontend.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.auth.RefreshResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;

class SessionStateTest {
    @Test
    void startsAndClearsSession() {
        var now = Instant.parse("2026-09-12T00:00:00Z");
        var state = new SessionState(Clock.fixed(now, ZoneOffset.UTC));
        var user = new User(4, "employee01", "employee01@resolveit.local",
                Role.EMPLOYEE, true, "2026-09-04T00:00:00Z", "2026-09-04T00:00:00Z");

        state.start(new LoginResponse("secret-token", "refresh-token", "Bearer", 900, 604_800, user));

        assertEquals(user, state.current().orElseThrow().user());
        assertEquals("secret-token", state.current().orElseThrow().accessToken());
        assertEquals("refresh-token", state.current().orElseThrow().refreshToken());
        assertEquals(now.plusSeconds(900), state.current().orElseThrow().accessExpiresAt());
        assertEquals(now.plusSeconds(604_800), state.current().orElseThrow().refreshExpiresAt());

        state.refresh(new RefreshResponse("new-access", "new-refresh", "Bearer", 900, 604_800));

        assertEquals(user, state.current().orElseThrow().user());
        assertEquals("new-access", state.current().orElseThrow().accessToken());
        assertEquals("new-refresh", state.current().orElseThrow().refreshToken());

        state.clear();
        assertTrue(state.current().isEmpty());
    }
}
