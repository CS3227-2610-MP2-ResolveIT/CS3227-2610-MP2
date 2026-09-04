package resolveit.frontend.ticket;

public final class TicketFailure extends RuntimeException {
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, INVALID_REQUEST, CONNECTION, TIMEOUT, SERVER, INVALID_RESPONSE }

    private final Kind kind;
    private final String code;

    TicketFailure(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    TicketFailure(Kind kind, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() { return kind; }
    public String code() { return code; }
}
