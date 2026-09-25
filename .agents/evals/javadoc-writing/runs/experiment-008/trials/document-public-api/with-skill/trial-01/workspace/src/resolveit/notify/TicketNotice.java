package resolveit.notify;

/**
 * Formats a short notice shown when a ticket is created.
 */
public final class TicketNotice {
    /**
     * Returns a confirmation message for a newly created ticket.
     *
     * @param ticketId the new ticket's identifier
     * @return the confirmation message
     */
    public String created(long ticketId) {
        return "Ticket #" + ticketId + " created";
    }
}
