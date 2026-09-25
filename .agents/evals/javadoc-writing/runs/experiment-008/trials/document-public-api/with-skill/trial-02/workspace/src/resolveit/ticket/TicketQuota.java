package resolveit.ticket;

import java.util.List;

/** Enforces the daily limit on tickets that count against the quota. */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a quota with the specified daily limit.
     *
     * @param dailyLimit the maximum number of counted tickets allowed per day
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Returns the number of additional counted tickets that can be created today.
     *
     * @param todaysTickets the tickets already created today
     * @return the remaining quota, or zero when the daily limit has been reached
     */
    public int remaining(List<Ticket> todaysTickets) {
        int used = 0;
        for (Ticket ticket : todaysTickets) {
            if (ticket.countsAgainstQuota()) {
                used++;
            }
        }
        return Math.max(dailyLimit - used, 0);
    }

    /**
     * Confirms that another counted ticket can be created today.
     *
     * @param todaysTickets the tickets already created today
     * @throws QuotaExceededException if the daily quota has been reached
     */
    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
