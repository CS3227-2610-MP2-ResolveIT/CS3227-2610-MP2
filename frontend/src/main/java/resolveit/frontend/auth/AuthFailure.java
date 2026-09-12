package resolveit.frontend.auth;

/** Safe client-facing authentication failure. */
public final class AuthFailure extends RuntimeException {
    /** Classifies authentication failures without exposing sensitive details. */
    public enum Kind {
        /** Submitted credentials were rejected. */
        INVALID_CREDENTIALS,
        /** Too many recent credential failures were submitted. */
        RATE_LIMITED,
        /** The authentication request was malformed. */
        INVALID_REQUEST,
        /** The backend could not be reached. */
        CONNECTION,
        /** The backend did not respond within the configured timeout. */
        TIMEOUT,
        /** The backend returned a server-side failure. */
        SERVER,
        /** The current credentials can no longer renew the session. */
        SESSION_EXPIRED,
        /** The backend returned an invalid success payload. */
        INVALID_RESPONSE
    }

    private final Kind kind;

    AuthFailure(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    AuthFailure(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Returns the stable classification used by presentation logic.
     *
     * @return stable failure classification for presentation logic
     */
    public Kind kind() {
        return kind;
    }
}
