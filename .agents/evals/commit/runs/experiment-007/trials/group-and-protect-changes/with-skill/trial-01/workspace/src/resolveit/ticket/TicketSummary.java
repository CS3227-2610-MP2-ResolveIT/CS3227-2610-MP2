package resolveit.ticket;

public final class TicketSummary {
    private TicketSummary() {}

    public static String format(String subject, String priority) {
        return "[" + priority + "] " + subject;
    }
}
