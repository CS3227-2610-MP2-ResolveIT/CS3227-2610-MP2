package resolveit.ticket;

public final class TicketLabels {
    private TicketLabels() {}

    public static String reference(long id) {
        return "TICKET-" + id;
    }
}
