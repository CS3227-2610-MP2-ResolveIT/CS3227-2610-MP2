package resolveit.auth;

/**
 * Derives the account lockout state from a running count of failed logins.
 */
public final class LockoutPolicy {
    private final int maxFailures;

    /**
     * Creates a lockout policy.
     *
     * @param maxFailures the number of failures that triggers a lockout
     */
    public LockoutPolicy(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    /**
     * Returns whether the account is locked out for the given failure count.
     *
     * @param failures the number of consecutive failed logins
     * @return true if the account should be locked out
     */
    public boolean isLockedOut(int failures) {
        return failures >= maxFailures;
    }

    /**
     * Returns the failures remaining before lockout.
     *
     * @param failures the number of consecutive failed logins
     * @return the remaining attempts, never negative
     */
    public int attemptsLeft(int failures) {
        return Math.max(maxFailures - failures, 0);
    }
}
