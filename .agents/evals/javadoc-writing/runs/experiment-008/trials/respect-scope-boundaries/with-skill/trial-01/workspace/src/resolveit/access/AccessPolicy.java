package resolveit.access;

/** Defines the attempt limits used to determine whether access is locked out. */
public final class AccessPolicy {
    private final int maxAttempts;

    /**
     * Creates an access policy with the specified maximum number of attempts.
     *
     * @param maxAttempts maximum number of attempts allowed before lockout.
     */
    public AccessPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Returns whether the number of failed attempts has reached the lockout limit.
     *
     * @param failedAttempts number of failed attempts.
     * @return {@code true} if the failed attempts are at least the maximum allowed;
     *         {@code false} otherwise.
     */
    public boolean isLockedOut(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }

    /**
     * Returns the number of attempts remaining before lockout, never below zero.
     *
     * @param failedAttempts number of failed attempts.
     * @return the number of remaining attempts.
     */
    public int remainingAttempts(int failedAttempts) {
        int remaining = maxAttempts - failedAttempts;
        return Math.max(remaining, 0);
    }
}
