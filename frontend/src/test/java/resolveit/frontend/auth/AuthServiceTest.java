package resolveit.frontend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;

class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void trimsEmailAndStoresSuccessfulSession() {
        var session = session();
        var client = new StubAuthClient();

        new AuthService(client, session)
                .login("  manager@resolveit.local ", "password")
                .toCompletableFuture().join();

        assertEquals("manager@resolveit.local", client.loginRequest.email());
        assertEquals(client.user, session.current().orElseThrow().user());
    }

    @Test
    void rateLimitedLoginDoesNotCreateSession() {
        var session = session();
        var client = new StubAuthClient();
        client.loginFuture = CompletableFuture.failedFuture(new AuthFailure(
                AuthFailure.Kind.RATE_LIMITED,
                "Too many sign-in attempts. Please wait 1 minute before trying again."));

        try {
            service(client, session).login("employee@test.local", "wrong")
                    .toCompletableFuture().join();
            throw new AssertionError("Expected login to fail");
        } catch (java.util.concurrent.CompletionException expected) {
            assertTrue(expected.getCause() instanceof AuthFailure);
        }

        assertTrue(session.current().isEmpty());
    }

    @Test
    void validSessionDoesNotRefreshAnUnexpiredAccessToken() {
        var session = session();
        var client = new StubAuthClient();
        session.start(client.loginResponse(900));
        var service = service(client, session);

        var current = service.validSession().toCompletableFuture().join();

        assertEquals("access", current.accessToken());
        assertEquals(0, client.refreshCalls);
    }

    @Test
    void concurrentRequestsShareOneInFlightRefresh() {
        var session = session();
        var client = new StubAuthClient();
        session.start(client.loginResponse(10));
        client.refreshFuture = new CompletableFuture<>();
        var service = service(client, session);

        var first = service.validSession().toCompletableFuture();
        var second = service.validSession().toCompletableFuture();

        assertEquals(1, client.refreshCalls);
        assertSame(first, second);
        client.refreshFuture.complete(new RefreshResponse(
                "new-access", "new-refresh", "Bearer", 900, 604_800));
        assertEquals("new-access", first.join().accessToken());
        assertEquals("new-access", second.join().accessToken());
    }

    @Test
    void failedRefreshClearsSession() {
        var session = session();
        var client = new StubAuthClient();
        session.start(client.loginResponse(10));
        client.refreshFuture = CompletableFuture.failedFuture(new AuthFailure(
                AuthFailure.Kind.SESSION_EXPIRED, "Session expired."));
        var service = service(client, session);

        try {
            service.validSession().toCompletableFuture().join();
            throw new AssertionError("Expected refresh to fail");
        } catch (java.util.concurrent.CompletionException expected) {
            assertTrue(expected.getCause() instanceof AuthFailure);
        }
        assertTrue(session.current().isEmpty());
    }

    @Test
    void logoutSendsCurrentCredentialsAndAlwaysClearsSession() {
        var session = session();
        var client = new StubAuthClient();
        session.start(client.loginResponse(900));
        client.logoutFuture = CompletableFuture.failedFuture(new AuthFailure(
                AuthFailure.Kind.CONNECTION, "Offline."));

        service(client, session).logout().toCompletableFuture().join();

        assertEquals("refresh", client.logoutRequest.refreshToken());
        assertEquals("Bearer access", client.logoutAuthorization);
        assertTrue(session.current().isEmpty());
    }

    @Test
    void logoutRefreshesAnAccessTokenThatIsNearExpiry() {
        var session = session();
        var client = new StubAuthClient();
        session.start(client.loginResponse(10));

        service(client, session).logout().toCompletableFuture().join();

        assertEquals(1, client.refreshCalls);
        assertEquals("new-refresh", client.logoutRequest.refreshToken());
        assertEquals("Bearer new-access", client.logoutAuthorization);
        assertTrue(session.current().isEmpty());
    }

    private SessionState session() {
        return new SessionState(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthService service(AuthClient client, SessionState session) {
        return new AuthService(client, session, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class StubAuthClient implements AuthClient {
        private final User user = new User(1, "manager", "manager@resolveit.local",
                Role.MANAGER, true, null, null);
        private LoginRequest loginRequest;
        private int refreshCalls;
        private RefreshRequest refreshRequest;
        private LogoutRequest logoutRequest;
        private String logoutAuthorization;
        private CompletableFuture<LoginResponse> loginFuture;
        private CompletableFuture<RefreshResponse> refreshFuture = CompletableFuture.completedFuture(
                new RefreshResponse("new-access", "new-refresh", "Bearer", 900, 604_800));
        private CompletableFuture<Void> logoutFuture = CompletableFuture.completedFuture(null);

        private LoginResponse loginResponse(long accessExpiresIn) {
            return new LoginResponse("access", "refresh", "Bearer", accessExpiresIn, 604_800, user);
        }

        @Override
        public CompletionStage<LoginResponse> login(LoginRequest request) {
            loginRequest = request;
            return loginFuture == null ? CompletableFuture.completedFuture(loginResponse(900)) : loginFuture;
        }

        @Override
        public CompletionStage<RefreshResponse> refresh(RefreshRequest request) {
            refreshCalls++;
            refreshRequest = request;
            return refreshFuture;
        }

        @Override
        public CompletionStage<Void> logout(LogoutRequest request, String authorization) {
            logoutRequest = request;
            logoutAuthorization = authorization;
            return logoutFuture;
        }
    }
}
