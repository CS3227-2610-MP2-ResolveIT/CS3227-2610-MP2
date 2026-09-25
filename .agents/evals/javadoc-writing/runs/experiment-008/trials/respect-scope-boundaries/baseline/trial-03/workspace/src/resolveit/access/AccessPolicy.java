package resolveit.access;

/**
 * Defines the failed-attempt threshold used to determine access lockout.
 */
public final class AccessPolicy {
    private final int maxAttempts;

    /**
     * Creates an access policy with the specified maximum number of attempts.
     *
     * @param maxAttempts the number of failed attempts at which access is locked
     *                    out
     */
    public AccessPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Determines whether the specified number of failed attempts has reached
     * the lockout threshold.
     *
     * @param failedAttempts the number of failed attempts
     * @return {@code true} if access is locked out; {@code false} otherwise
     */
    public boolean isLockedOut(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }

    /**
     * Calculates how many attempts remain before access is locked out.
     *
     * @param failedAttempts the number of failed attempts
     * @return the number of remaining attempts, or {@code 0} if the threshold
     *         has already been reached
     */
    public int remainingAttempts(int failedAttempts) {
        int remaining = maxAttempts - failedAttempts;
        return Math.max(remaining, 0);
    }
}
