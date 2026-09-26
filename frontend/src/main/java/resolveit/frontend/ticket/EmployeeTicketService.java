package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;

/**
 * Employee-facing wrapper over {@link TicketClient} that exposes the raise,
 * track, and comment actions available to a requester and trims comment text.
 */
public final class EmployeeTicketService {
    private final TicketClient client;

    /**
     * Creates the service over a ticket client.
     *
     * @param client asynchronous ticket API client
     */
    public EmployeeTicketService(TicketClient client) {
        this.client = client;
    }

    /**
     * Lists the employee's own tickets, optionally filtered by status.
     *
     * @param status optional status filter
     * @param page zero-based page index
     * @return a stage completing with a page of tickets
     */
    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page) {
        return client.list(status, page, 20);
    }
    /**
     * Loads one ticket visible to the employee.
     *
     * @param id ticket identifier
     * @return a stage completing with the ticket details
     */
    public CompletionStage<Ticket> get(int id) { return client.get(id); }

    /**
     * Creates a ticket owned by the current employee.
     *
     * @param request ticket fields to submit
     * @return a stage completing with the created ticket
     */
    public CompletionStage<Ticket> create(CreateTicket request) { return client.create(request); }

    /**
     * Updates permitted fields of an employee-owned ticket.
     *
     * @param id ticket identifier
     * @param request fields and version expected by the update
     * @return a stage completing with the updated ticket
     */
    public CompletionStage<Ticket> update(int id, UpdateTicket request) { return client.update(id, request); }

    /**
     * Loads the messages visible to the employee for a ticket.
     *
     * @param id ticket identifier
     * @return a stage completing with the visible messages
     */
    public CompletionStage<PageResponse<TicketMessage>> messages(int id) { return client.messages(id); }

    /**
     * Adds a trimmed public comment to a ticket.
     *
     * @param id ticket identifier
     * @param message comment body, trimmed before sending
     * @return a stage completing with the created message
     */
    public CompletionStage<TicketMessage> addComment(int id, String message) {
        return client.addComment(id, new CreateMessage("PUBLIC_COMMENT", message.trim()));
    }
    /**
     * Cancels an eligible ticket owned by the employee.
     *
     * @param id ticket identifier
     * @return a stage completing with the cancelled ticket
     */
    public CompletionStage<Ticket> cancel(int id) { return client.cancel(id); }

    /**
     * Reopens a resolved ticket owned by the employee.
     *
     * @param id ticket identifier
     * @return a stage completing with the reopened ticket
     */
    public CompletionStage<Ticket> reopen(int id) { return client.reopen(id); }
}
