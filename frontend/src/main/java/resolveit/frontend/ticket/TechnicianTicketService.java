package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;

public final class TechnicianTicketService {
    private final TicketClient client;

    public TechnicianTicketService(TicketClient client) {
        this.client = client;
    }

    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                       AssignmentFilter assignment, int page) {
        return client.list(status, priority, assignment == AssignmentFilter.MINE ? true : null,
                assignment == AssignmentFilter.UNASSIGNED ? true : null, page, 20);
    }

    public CompletionStage<Ticket> get(int id) { return client.get(id); }
    public CompletionStage<PageResponse<TicketMessage>> messages(int id) { return client.messages(id); }
    public CompletionStage<Ticket> take(int id) { return client.take(id); }
    public CompletionStage<Ticket> beginWork(int id) {
        return client.changeStatus(id, new ChangeStatus(TicketStatus.IN_PROGRESS));
    }
    public CompletionStage<Ticket> changePriority(int id, TicketPriority priority) {
        return client.changePriority(id, new ChangePriority(priority));
    }
    public CompletionStage<TicketMessage> addMessage(int id, MessageType type, String message) {
        return client.addComment(id, new CreateMessage(type.name(), message.trim()));
    }
    public CompletionStage<Ticket> resolve(int id, String resolutionNote) {
        return client.resolve(id, new ResolveTicket(resolutionNote.trim()));
    }
    public CompletionStage<Ticket> cancel(int id) { return client.cancel(id); }
    public CompletionStage<Ticket> reopen(int id) { return client.reopen(id); }

    public enum AssignmentFilter {
        ALL("All visible"), UNASSIGNED("Unassigned"), MINE("Assigned to me");

        private final String displayName;
        AssignmentFilter(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum MessageType {
        PUBLIC_COMMENT("Public comment"), INTERNAL_NOTE("Internal note");

        private final String displayName;
        MessageType(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
