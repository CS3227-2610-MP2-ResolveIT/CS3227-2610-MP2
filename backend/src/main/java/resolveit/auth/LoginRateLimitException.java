package resolveit.auth;

/** Signals that one login key must wait before another authentication attempt. */
public final class LoginRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    LoginRateLimitException(long retryAfterSeconds) {
        super("Too many sign-in attempts. Please try again later.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /**
     * Returns the whole seconds the client should wait before retrying.
     *
     * @return positive retry delay in seconds
     */
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
