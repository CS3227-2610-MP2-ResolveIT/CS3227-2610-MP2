package resolveit.frontend.auth;

public final class AuthFailure extends RuntimeException {
    public enum Kind {
        INVALID_CREDENTIALS,
        INVALID_REQUEST,
        CONNECTION,
        TIMEOUT,
        SERVER,
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

    public Kind kind() {
        return kind;
    }
}
