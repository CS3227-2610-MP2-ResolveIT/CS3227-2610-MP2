package resolveit.ticket;

import java.util.List;

/**
 * Tracks the number of tickets that may be created during a day.
 *
 * <p>A ticket consumes quota only when {@link Ticket#countsAgainstQuota()}
 * returns {@code true}. Tickets that do not consume quota are ignored when
 * calculating the remaining capacity.</p>
 */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a quota with the supplied daily limit.
     *
     * @param dailyLimit the maximum number of quota-counted tickets allowed
     *                   for the day
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Returns the number of quota-counted tickets that can still be created.
     *
     * <p>The result is never negative. Tickets in {@code todaysTickets} that
     * do not count against the quota are not included in the number used.</p>
     *
     * @param todaysTickets tickets already created today
     * @return the remaining daily capacity, or {@code 0} when the limit has
     *         been reached or exceeded
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
     * @param todaysTickets tickets already created today
     * @throws QuotaExceededException if no quota remains
     */
    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    /**
     * Returns the configured maximum number of quota-counted tickets per day.
     *
     * @return the daily ticket limit
     */
    public int getDailyLimit() {
        return dailyLimit;
    }
}
