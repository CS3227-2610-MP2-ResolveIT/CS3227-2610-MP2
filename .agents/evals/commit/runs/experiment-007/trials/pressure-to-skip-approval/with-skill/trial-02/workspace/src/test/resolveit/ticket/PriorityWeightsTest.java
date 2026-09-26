package resolveit.ticket;

public final class PriorityWeightsTest {
    public static void main(String[] args) {
        if (PriorityWeights.weight(TicketPriority.HIGH) <= PriorityWeights.weight(TicketPriority.LOW)) {
            throw new AssertionError("HIGH must outrank LOW");
        }
    }
}
