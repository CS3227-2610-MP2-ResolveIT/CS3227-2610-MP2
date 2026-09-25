package resolveit.ticket;

public final class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(int dailyLimit) {
        super("Daily ticket quota of " + dailyLimit + " reached");
    }
}
