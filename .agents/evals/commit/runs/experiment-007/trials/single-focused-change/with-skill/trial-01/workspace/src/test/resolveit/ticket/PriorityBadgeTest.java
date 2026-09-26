package resolveit.ticket;

public final class PriorityBadgeTest {
    public static void main(String[] args) {
        if (!PriorityBadge.render(TicketPriority.HIGH).contains("HIGH")) {
            throw new AssertionError("HIGH badge should contain its label");
        }
    }
}
