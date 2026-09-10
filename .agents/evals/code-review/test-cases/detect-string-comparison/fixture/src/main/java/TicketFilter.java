import java.util.List;

/** Filters tickets for display in the support queue. */
public final class TicketFilter {
    private TicketFilter() {
    }

    /** A ticket identifier and its current status. */
    public record Ticket(String id, String status) {
    }

    /** Returns tickets whose status matches the requested status. */
    public static List<Ticket> byStatus(List<Ticket> tickets, String status) {
        return tickets.stream()
                .filter(ticket -> ticket.status() == status)
                .toList();
    }
}
