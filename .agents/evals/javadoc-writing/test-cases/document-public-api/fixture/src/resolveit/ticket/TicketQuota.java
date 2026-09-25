package resolveit.ticket;

import java.util.List;

public final class TicketQuota {
    private final int dailyLimit;

    public TicketQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int remaining(List<Ticket> todaysTickets) {
        int used = 0;
        for (Ticket ticket : todaysTickets) {
            if (ticket.countsAgainstQuota()) {
                used++;
            }
        }
        return Math.max(dailyLimit - used, 0);
    }

    public void assertCanCreate(List<Ticket> todaysTickets) {
        if (remaining(todaysTickets) <= 0) {
            throw new QuotaExceededException(dailyLimit);
        }
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
