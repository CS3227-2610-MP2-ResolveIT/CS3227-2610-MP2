package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;

public final class EmployeeTicketService {
    private final TicketClient client;

    public EmployeeTicketService(TicketClient client) {
        this.client = client;
    }

    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page) {
        return client.list(status, page, 20);
    }
    public CompletionStage<Ticket> get(int id) { return client.get(id); }
    public CompletionStage<Ticket> create(CreateTicket request) { return client.create(request); }
    public CompletionStage<Ticket> update(int id, UpdateTicket request) { return client.update(id, request); }
    public CompletionStage<PageResponse<TicketMessage>> messages(int id) { return client.messages(id); }
    public CompletionStage<TicketMessage> addComment(int id, String message) {
        return client.addComment(id, new CreateMessage("PUBLIC_COMMENT", message.trim()));
    }
    public CompletionStage<Ticket> cancel(int id) { return client.cancel(id); }
    public CompletionStage<Ticket> reopen(int id) { return client.reopen(id); }
}
