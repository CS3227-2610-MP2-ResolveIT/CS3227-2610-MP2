package resolveit.ticket;

import java.util.List;

/** Enforces the daily limit on tickets that count against the quota. */
public final class TicketQuota {
    private final int dailyLimit;

    /**
     * Creates a quota with the specified daily limit.
     *
     * @param dailyLimit the maximum number of tickets that may count against the quota
     */
    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Returns the number of quota slots remaining after today's tickets are counted.
     *
     * @param todaysTickets the tickets created today
     * @return the number of remaining quota slots, or zero when the quota has been reached
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
     * Confirms that another ticket can be created under the daily quota.
     *
     * @param todaysTickets the tickets created today
     * @throws QuotaExceededException if no quota slots remain
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
