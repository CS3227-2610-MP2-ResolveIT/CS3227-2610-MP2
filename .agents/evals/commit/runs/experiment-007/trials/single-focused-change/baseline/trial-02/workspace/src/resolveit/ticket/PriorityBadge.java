package resolveit.ticket;

/** Renders a ticket priority as a coloured badge for the ticket list. */
public final class PriorityBadge {
    private PriorityBadge() {}

    public static String render(TicketPriority priority) {
        switch (priority) {
            case HIGH:
                return "\uD83D\uDD34 HIGH";
            case MEDIUM:
                return "\uD83D\uDFE1 MEDIUM";
            case LOW:
                return "\uD83D\uDFE2 LOW";
            default:
                throw new IllegalArgumentException("Unknown priority: " + priority);
        }
    }
}
