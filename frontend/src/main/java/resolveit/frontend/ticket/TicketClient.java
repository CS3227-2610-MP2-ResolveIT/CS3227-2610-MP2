package resolveit.frontend.ticket;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;

/**
 * Asynchronous boundary to the backend ticket API. Each method completes on a
 * background thread; the returned stage fails with a ticket or auth failure when
 * the request cannot be fulfilled. Results are scoped to the authenticated
 * caller's role on the server.
 */
public interface TicketClient {
    /**
     * Lists tickets filtered by status.
     *
     * @param status optional status filter, or {@code null} for all
     * @param page zero-based page index
     * @param size page size
     * @return a stage completing with a page of tickets
     */
    CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size);

    /**
     * Lists tickets with status, priority, and assignment filters.
     *
     * @param status optional status filter
     * @param priority optional priority filter
     * @param assignedToMe optional filter for the caller's assignments
     * @param unassigned optional filter for unassigned tickets
     * @param page zero-based page index
     * @param size page size
     * @return a stage completing with a page of tickets
     */
    CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                Boolean assignedToMe, Boolean unassigned, int page, int size);

    /**
     * Fetches a single ticket.
     *
     * @param ticketId ticket identifier
     * @return a stage completing with the ticket
     */
    CompletionStage<Ticket> get(int ticketId);

    /**
     * Creates a ticket owned by the caller.
     *
     * @param request ticket details
     * @return a stage completing with the created ticket
     */
    CompletionStage<Ticket> create(CreateTicket request);

    /**
     * Applies an optimistically versioned edit to a ticket.
     *
     * @param ticketId ticket identifier
     * @param request editable fields and expected version
     * @return a stage completing with the updated ticket
     */
    CompletionStage<Ticket> update(int ticketId, UpdateTicket request);

    /**
     * Lists a ticket's messages visible to the caller.
     *
     * @param ticketId ticket identifier
     * @return a stage completing with a page of messages
     */
    CompletionStage<PageResponse<TicketMessage>> messages(int ticketId);

    /**
     * Adds a message to a ticket.
     *
     * @param ticketId ticket identifier
     * @param request message content and visibility type
     * @return a stage completing with the created message
     */
    CompletionStage<TicketMessage> addComment(int ticketId, CreateMessage request);

    /**
     * Cancels a ticket.
     *
     * @param ticketId ticket identifier
     * @return a stage completing with the cancelled ticket
     */
    CompletionStage<Ticket> cancel(int ticketId);

    /**
     * Reopens a resolved ticket.
     *
     * @param ticketId ticket identifier
     * @return a stage completing with the reopened ticket
     */
    CompletionStage<Ticket> reopen(int ticketId);

    /**
     * Self-assigns an open, unassigned ticket to the calling support agent.
     *
     * @param ticketId ticket identifier
     * @return a stage completing with the taken ticket
     */
    CompletionStage<Ticket> take(int ticketId);

    /**
     * Changes a ticket's workflow status.
     *
     * @param ticketId ticket identifier
     * @param request requested status
     * @return a stage completing with the updated ticket
     */
    CompletionStage<Ticket> changeStatus(int ticketId, ChangeStatus request);

    /**
     * Changes a ticket's priority.
     *
     * @param ticketId ticket identifier
     * @param request new priority
     * @return a stage completing with the updated ticket
     */
    CompletionStage<Ticket> changePriority(int ticketId, ChangePriority request);

    /**
     * Resolves an in-progress ticket with a resolution note.
     *
     * @param ticketId ticket identifier
     * @param request resolution note
     * @return a stage completing with the resolved ticket
     */
    CompletionStage<Ticket> resolve(int ticketId, ResolveTicket request);
}
