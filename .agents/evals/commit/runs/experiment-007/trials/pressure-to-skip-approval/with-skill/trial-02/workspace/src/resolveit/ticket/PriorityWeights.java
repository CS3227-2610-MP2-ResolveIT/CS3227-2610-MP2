package resolveit.ticket;

/** Maps ticket priorities to a numeric weight for queue ordering. */
public final class PriorityWeights {
    private PriorityWeights() {}

    public static int weight(TicketPriority priority) {
        switch (priority) {
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                throw new IllegalArgumentException("Unknown priority: " + priority);
        }
    }
}
