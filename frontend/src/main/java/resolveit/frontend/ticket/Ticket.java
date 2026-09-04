package resolveit.frontend.ticket;

public record Ticket(
        int id,
        String ticketNumber,
        String subject,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        int requesterId,
        String requesterUsername,
        Integer assignedToId,
        String assignedToUsername,
        String resolutionNote,
        String createdAt,
        String updatedAt,
        String resolvedAt,
        int version) {
    public boolean isEditableByRequester() {
        return status == TicketStatus.OPEN && assignedToId == null;
    }

    public boolean isCancellableByRequester() {
        return status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS;
    }

    public boolean isReopenableByRequester() {
        return status == TicketStatus.RESOLVED;
    }
}
