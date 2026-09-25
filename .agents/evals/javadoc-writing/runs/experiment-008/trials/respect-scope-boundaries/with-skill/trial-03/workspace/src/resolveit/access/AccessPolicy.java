package resolveit.access;

/**
 * Defines the maximum number of failed attempts allowed before access is locked out.
 */
public final class AccessPolicy {
    private final int maxAttempts;

    /**
     * Creates an access policy with the specified failed-attempt limit.
     *
     * @param maxAttempts maximum number of failed attempts allowed
     */
    public AccessPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Returns whether the failed-attempt limit has been reached.
     *
     * @param failedAttempts number of failed attempts
     * @return true if access should be locked out, and false otherwise
     */
    public boolean isLockedOut(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }

    /**
     * Returns the number of failed attempts remaining before lockout.
     *
     * @param failedAttempts number of failed attempts
     * @return the non-negative number of attempts remaining
     */
    public int remainingAttempts(int failedAttempts) {
        int remaining = maxAttempts - failedAttempts;
        return Math.max(remaining, 0);
    }
}
