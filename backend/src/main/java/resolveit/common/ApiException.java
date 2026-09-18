package resolveit.common;

import org.springframework.http.HttpStatus;

/** Application exception that carries the HTTP status and stable error code to return. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    /**
     * Creates a 400 Bad Request failure.
     *
     * @param code stable client-facing error code
     * @param message human-readable message
     * @return the exception to throw
     */
    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    /**
     * Creates a 401 Unauthorized failure.
     *
     * @param code stable client-facing error code
     * @param message human-readable message
     * @return the exception to throw
     */
    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    /**
     * Creates a 403 Forbidden failure.
     *
     * @param code stable client-facing error code
     * @param message human-readable message
     * @return the exception to throw
     */
    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    /**
     * Creates a 404 Not Found failure.
     *
     * @param code stable client-facing error code
     * @param message human-readable message
     * @return the exception to throw
     */
    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    /**
     * Creates a 409 Conflict failure.
     *
     * @param code stable client-facing error code
     * @param message human-readable message
     * @return the exception to throw
     */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
