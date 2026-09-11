public final class TicketSummaryTest {
    public static void main(String[] args) {
        String actual = TicketSummary.format("VPN unavailable", "HIGH");
        if (!"VPN unavailable".equals(actual)) {
            throw new AssertionError("Unexpected summary: " + actual);
        }
    }
}

