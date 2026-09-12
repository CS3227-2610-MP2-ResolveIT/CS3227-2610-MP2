package resolveit.frontend.auth;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.session.SessionState.Session;

/** Supplies a valid authenticated session and coordinates forced renewal. */
public interface AuthenticatedSession {
    /**
     * Returns the current session, renewing credentials first when the access
     * token is near expiry.
     *
     * @return a stage containing a usable session, or failing when renewal is
     *         unavailable
     */
    CompletionStage<Session> validSession();

    /**
     * Rotates the current refresh token, sharing an in-flight operation among
     * concurrent callers.
     *
     * @return a stage containing the session with replacement credentials
     */
    CompletionStage<Session> refreshSession();
}
