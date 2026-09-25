package resolveit.access;

public final class AccessPolicy {
    private final int maxAttempts;

    public AccessPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isLockedOut(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }

    public int remainingAttempts(int failedAttempts) {
        int remaining = maxAttempts - failedAttempts;
        return Math.max(remaining, 0);
    }
}
