package resolveit.ticket;

import java.util.List;

/**
 * Tracks the maximum number of quota-counted tickets that may be created in a
 * day.
 */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a ticket quota with the specified daily limit.
     *
     * @param dailyLimit the maximum number of tickets that may count against
     *                    the quota
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Calculates how many quota slots remain after accounting for the supplied
     * tickets.
     *
     * <p>Only tickets whose {@link Ticket#countsAgainstQuota()} value is
     * {@code true} are counted. The returned value is never negative.</p>
     *
     * @param todaysTickets tickets already created today
     * @return the number of quota slots remaining
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
     * @throws QuotaExceededException if no quota slots remain
     */
    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    /**
     * Returns the maximum number of tickets that may count against the quota
     * in a day.
     *
     * @return the daily ticket limit
     */
    public int getDailyLimit() {
        return dailyLimit;
    }
}
