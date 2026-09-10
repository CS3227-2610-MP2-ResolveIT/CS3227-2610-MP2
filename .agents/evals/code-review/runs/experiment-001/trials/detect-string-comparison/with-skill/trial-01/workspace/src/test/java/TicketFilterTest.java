import java.util.List;

/** Minimal executable tests for {@link TicketFilter}. */
public final class TicketFilterTest {
    private TicketFilterTest() {
    }

    public static void main(String[] args) {
        var tickets = List.of(
                new TicketFilter.Ticket("T1", "OPEN"),
                new TicketFilter.Ticket("T2", "CLOSED"),
                new TicketFilter.Ticket("T3", "OPEN"));

        var result = TicketFilter.byStatus(tickets, "OPEN");

        if (!result.equals(List.of(tickets.get(0), tickets.get(2)))) {
            throw new AssertionError("Expected the two OPEN tickets in source order");
        }
        System.out.println("TicketFilterTest passed");
    }
}
