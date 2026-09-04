package resolveit.frontend.ticket;

public record TicketMessage(
        int id,
        int ticketId,
        int authorId,
        String authorUsername,
        String messageType,
        String message,
        String createdAt) {
}
