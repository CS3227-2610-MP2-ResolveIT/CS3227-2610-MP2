package resolveit.access;

public final class SessionClock {
    private long lastActivityMillis;

    public SessionClock(long startMillis) {
        this.lastActivityMillis = startMillis;
    }

    public void touch(long nowMillis) {
        this.lastActivityMillis = nowMillis;
    }

    public boolean isExpired(long nowMillis, long timeoutMillis) {
        return nowMillis - lastActivityMillis > timeoutMillis;
    }
}
