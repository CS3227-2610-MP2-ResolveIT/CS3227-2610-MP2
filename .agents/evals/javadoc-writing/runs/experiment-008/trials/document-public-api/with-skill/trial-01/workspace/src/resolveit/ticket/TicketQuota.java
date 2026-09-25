package resolveit.ticket;

import java.util.List;

/**
 * Tracks the daily limit for tickets that count against the quota.
 */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a quota with the specified daily limit.
     *
     * @param dailyLimit the maximum number of tickets allowed per day
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Returns the number of tickets that can still be created today.
     *
     * @param todaysTickets the tickets already created today
     * @return the non-negative number of remaining tickets
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
     * Confirms that another ticket can be created under this quota.
     *
     * @param todaysTickets the tickets already created today
     * @throws QuotaExceededException if the daily quota has been reached
     */
    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    /**
     * Returns the maximum number of tickets allowed per day.
     *
     * @return the daily ticket limit
     */
    public int getDailyLimit() {
        return dailyLimit;
    }
}
