package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;

public interface TicketClient {
    CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size);
    CompletionStage<Ticket> get(int ticketId);
    CompletionStage<Ticket> create(CreateTicket request);
    CompletionStage<Ticket> update(int ticketId, UpdateTicket request);
    CompletionStage<PageResponse<TicketMessage>> messages(int ticketId);
    CompletionStage<TicketMessage> addComment(int ticketId, CreateMessage request);
    CompletionStage<Ticket> cancel(int ticketId);
    CompletionStage<Ticket> reopen(int ticketId);
}
