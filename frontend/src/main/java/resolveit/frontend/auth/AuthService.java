package resolveit.frontend.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.session.SessionState.Session;

/** Coordinates login, credential renewal, and secure sign-out. */
public final class AuthService implements AuthenticatedSession {
    private static final Duration REFRESH_SAFETY_MARGIN = Duration.ofSeconds(30);

    private final AuthClient authClient;
    private final SessionState session;
    private final Clock clock;
    private final Object refreshLock = new Object();
    private CompletableFuture<Session> refreshInFlight;

    /**
     * Creates an authentication service backed by the given client and state.
     *
     * @param authClient authentication server client
     * @param session in-memory session state
     */
    public AuthService(AuthClient authClient, SessionState session) {
        this(authClient, session, Clock.systemUTC());
    }

    AuthService(AuthClient authClient, SessionState session, Clock clock) {
        this.authClient = authClient;
        this.session = session;
        this.clock = clock;
    }

    /**
     * Authenticates the supplied credentials and starts an in-memory session.
     *
     * @param email submitted account email
     * @param password submitted password
     * @return a stage containing the successful login payload
     */
    public CompletionStage<LoginResponse> login(String email, String password) {
        var request = new LoginRequest(email.trim(), password);
        return authClient.login(request).thenApply(response -> {
            session.start(response);
            return response;
        });
    }

    /** Returns a session whose access token is not near expiry, refreshing it when required. */
    @Override
    public CompletionStage<Session> validSession() {
        var current = session.current().orElse(null);
        if (current == null) {
            return CompletableFuture.failedFuture(sessionExpired());
        }
        if (current.accessExpiresAt().isAfter(clock.instant().plus(REFRESH_SAFETY_MARGIN))) {
            return CompletableFuture.completedFuture(current);
        }
        return refreshSession();
    }

    /** Rotates the current refresh token, sharing one operation among concurrent callers. */
    @Override
    public CompletionStage<Session> refreshSession() {
        synchronized (refreshLock) {
            if (refreshInFlight != null) {
                return refreshInFlight;
            }

            var current = session.current().orElse(null);
            if (current == null || !current.refreshExpiresAt().isAfter(clock.instant())) {
                session.clear();
                return CompletableFuture.failedFuture(sessionExpired());
            }

            var operation = authClient.refresh(new RefreshRequest(current.refreshToken()))
                    .thenApply(session::refresh)
                    .toCompletableFuture();
            refreshInFlight = operation;
            operation.whenComplete((result, failure) -> completeRefresh(operation, failure));
            return operation;
        }
    }

    /**
     * Revokes the current refresh token and always clears local session state.
     *
     * @return a stage that completes after sign-out cleanup
     */
    public CompletionStage<Void> logout() {
        var current = session.current().orElse(null);
        if (current == null) {
            session.clear();
            return CompletableFuture.completedFuture(null);
        }

        return validSession()
                .thenCompose(active -> authClient.logout(
                        new LogoutRequest(active.refreshToken()), authorization(active)))
                .handle((ignored, failure) -> {
                    session.clear();
                    return null;
                });
    }

    private void completeRefresh(CompletableFuture<Session> operation, Throwable failure) {
        if (failure != null) {
            session.clear();
        }
        synchronized (refreshLock) {
            if (refreshInFlight == operation) {
                refreshInFlight = null;
            }
        }
    }

    private String authorization(Session current) {
        return current.tokenType() + " " + current.accessToken();
    }

    private AuthFailure sessionExpired() {
        return new AuthFailure(AuthFailure.Kind.SESSION_EXPIRED,
                "Your session has expired. Please sign in again.");
    }
}
