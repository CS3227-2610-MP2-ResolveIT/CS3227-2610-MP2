package resolveit.frontend.session;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.auth.RefreshResponse;
import resolveit.frontend.model.User;

/** Stores the current credentials and user in process memory. */
public final class SessionState {
    private final Clock clock;
    private Session session;

    /** Creates session state using the system UTC clock. */
    public SessionState() {
        this(Clock.systemUTC());
    }

    /**
     * Creates session state with an explicit clock for deterministic expiry
     * handling.
     *
     * @param clock clock used to calculate credential expiries
     */
    public SessionState(Clock clock) {
        this.clock = clock;
    }

    /**
     * Starts a session from a complete login response.
     *
     * @param response authenticated user and initial credentials
     */
    public synchronized void start(LoginResponse response) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                || response.refreshToken() == null || response.refreshToken().isBlank()
                || response.tokenType() == null || response.tokenType().isBlank()
                || response.expiresIn() <= 0 || response.refreshExpiresIn() <= 0
                || response.user() == null) {
            throw new IllegalArgumentException("A complete login response is required.");
        }
        var now = clock.instant();
        session = new Session(response.accessToken(), response.refreshToken(), response.tokenType(),
                now.plusSeconds(response.expiresIn()), now.plusSeconds(response.refreshExpiresIn()), response.user());
    }

    /**
     * Replaces rotated credentials while preserving the authenticated user.
     *
     * @param response replacement credentials
     * @return updated session
     */
    public synchronized Session refresh(RefreshResponse response) {
        if (session == null) {
            throw new IllegalStateException("No session is available to refresh.");
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                || response.refreshToken() == null || response.refreshToken().isBlank()
                || response.tokenType() == null || response.tokenType().isBlank()
                || response.expiresIn() <= 0 || response.refreshExpiresIn() <= 0) {
            throw new IllegalArgumentException("A complete refresh response is required.");
        }
        var now = clock.instant();
        session = new Session(response.accessToken(), response.refreshToken(), response.tokenType(),
                now.plusSeconds(response.expiresIn()), now.plusSeconds(response.refreshExpiresIn()), session.user());
        return session;
    }

    /**
     * Returns the current in-memory session when one exists.
     *
     * @return the current session, or an empty value when signed out
     */
    public synchronized Optional<Session> current() {
        return Optional.ofNullable(session);
    }

    /** Clears all in-memory session state. */
    public synchronized void clear() {
        session = null;
    }

    /**
     * Immutable in-memory authenticated session.
     *
     * @param accessToken short-lived bearer token
     * @param refreshToken opaque refresh credential
     * @param tokenType HTTP authorization scheme
     * @param accessExpiresAt access-token expiry
     * @param refreshExpiresAt refresh-token expiry
     * @param user authenticated user
     */
    public record Session(String accessToken, String refreshToken, String tokenType,
                          Instant accessExpiresAt, Instant refreshExpiresAt, User user) {
    }
}
