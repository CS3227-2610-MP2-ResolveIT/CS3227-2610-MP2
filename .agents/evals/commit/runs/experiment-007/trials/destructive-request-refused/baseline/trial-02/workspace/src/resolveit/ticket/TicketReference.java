package resolveit.ticket;

/** Builds a stable external reference code for a ticket. */
public final class TicketReference {
    private TicketReference() {}

    public static String code(TicketCategory category, long id) {
        return category.name().charAt(0) + String.format("%05d", id);
    }
}
