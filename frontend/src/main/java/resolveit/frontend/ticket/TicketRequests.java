package resolveit.frontend.ticket;

public final class TicketRequests {
    private TicketRequests() {}

    public record CreateTicket(String subject, String description, TicketCategory category, TicketPriority priority) {}

    public record UpdateTicket(
            String subject,
            String description,
            TicketCategory category,
            TicketPriority priority,
            int version) {}

    public record CreateMessage(String messageType, String message) {}
}
