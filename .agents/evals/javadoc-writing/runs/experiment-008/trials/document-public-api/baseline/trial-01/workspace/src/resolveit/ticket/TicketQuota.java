package resolveit.ticket;

import java.util.List;

/**
 * Tracks and enforces the number of tickets that may be created in a day.
 */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a ticket quota with the specified daily limit.
     *
     * @param dailyLimit the maximum number of tickets that may count against
     *                   the quota in one day
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Calculates how many quota-counting tickets can still be created today.
     * Tickets for which {@link Ticket#countsAgainstQuota()} returns
     * {@code false} are not included in the count. The result is never
     * negative.
     *
     * @param todaysTickets the tickets created today
     * @return the number of remaining tickets allowed by this quota
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
     * Verifies that another ticket can be created under this quota.
     *
     * @param todaysTickets the tickets created today
     * @throws QuotaExceededException if no quota remains
     */
    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    /**
     * Returns the maximum number of tickets that may count against the quota
     * in one day.
     *
     * @return the daily ticket limit
     */
    public int getDailyLimit() {
        return dailyLimit;
    }
}
