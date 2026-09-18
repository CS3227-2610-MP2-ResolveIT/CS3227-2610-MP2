package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;

/**
 * Technician-facing wrapper over {@link TicketClient} that maps workspace intent
 * (assignment filter, begin-work, resolve) onto the ticket API and trims text
 * input before sending it.
 */
public final class TechnicianTicketService {
    private final TicketClient client;

    /**
     * Creates the service over a ticket client.
     *
     * @param client asynchronous ticket API client
     */
    public TechnicianTicketService(TicketClient client) {
        this.client = client;
    }

    /**
     * Lists the technician queue, translating the assignment filter into the
     * client's mine/unassigned flags.
     *
     * @param status optional status filter
     * @param priority optional priority filter
     * @param assignment assignment scope to apply
     * @param page zero-based page index
     * @return a stage completing with a page of tickets
     */
    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                       AssignmentFilter assignment, int page) {
        return client.list(status, priority, assignment == AssignmentFilter.MINE ? true : null,
                assignment == AssignmentFilter.UNASSIGNED ? true : null, page, 20);
    }

    public CompletionStage<Ticket> get(int id) { return client.get(id); }
    public CompletionStage<PageResponse<TicketMessage>> messages(int id) { return client.messages(id); }
    public CompletionStage<Ticket> take(int id) { return client.take(id); }

    /**
     * Moves an assigned ticket into in-progress.
     *
     * @param id ticket identifier
     * @return a stage completing with the updated ticket
     */
    public CompletionStage<Ticket> beginWork(int id) {
        return client.changeStatus(id, new ChangeStatus(TicketStatus.IN_PROGRESS));
    }
    public CompletionStage<Ticket> changePriority(int id, TicketPriority priority) {
        return client.changePriority(id, new ChangePriority(priority));
    }

    /**
     * Adds a trimmed public comment or internal note to a ticket.
     *
     * @param id ticket identifier
     * @param type message visibility type
     * @param message message body, trimmed before sending
     * @return a stage completing with the created message
     */
    public CompletionStage<TicketMessage> addMessage(int id, MessageType type, String message) {
        return client.addComment(id, new CreateMessage(type.name(), message.trim()));
    }

    /**
     * Resolves a ticket with a trimmed resolution note.
     *
     * @param id ticket identifier
     * @param resolutionNote resolution note, trimmed before sending
     * @return a stage completing with the resolved ticket
     */
    public CompletionStage<Ticket> resolve(int id, String resolutionNote) {
        return client.resolve(id, new ResolveTicket(resolutionNote.trim()));
    }
    public CompletionStage<Ticket> cancel(int id) { return client.cancel(id); }
    public CompletionStage<Ticket> reopen(int id) { return client.reopen(id); }

    /** Assignment scope offered in the technician queue filter. */
    public enum AssignmentFilter {
        ALL("All visible"), UNASSIGNED("Unassigned"), MINE("Assigned to me");

        private final String displayName;
        AssignmentFilter(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    /** Message visibility a technician may choose when posting to a ticket. */
    public enum MessageType {
        PUBLIC_COMMENT("Public comment"), INTERNAL_NOTE("Internal note");

        private final String displayName;
        MessageType(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
