package resolveit.ticket;

import static resolveit.ticket.TicketDtos.AssignTicketRequest;
import static resolveit.ticket.TicketDtos.ChangePriorityRequest;
import static resolveit.ticket.TicketDtos.ChangeStatusRequest;
import static resolveit.ticket.TicketDtos.CreateMessageRequest;
import static resolveit.ticket.TicketDtos.CreateTicketRequest;
import static resolveit.ticket.TicketDtos.MessageResponse;
import static resolveit.ticket.TicketDtos.PageResponse;
import static resolveit.ticket.TicketDtos.ResolveTicketRequest;
import static resolveit.ticket.TicketDtos.TicketResponse;
import static resolveit.ticket.TicketDtos.UpdateTicketRequest;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the ticket lifecycle, assignment, messaging, and query endpoints. */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    /**
     * Creates the ticket controller.
     *
     * @param ticketService ticket workflow service enforcing role and state rules
     */
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Creates a ticket owned by the authenticated requester.
     *
     * @param authentication authenticated account identity
     * @param request validated ticket details
     * @return 201 response with the created ticket and its location
     */
    @PostMapping
    public ResponseEntity<TicketResponse> create(Authentication authentication,
                                                  @Valid @RequestBody CreateTicketRequest request) {
        var created = ticketService.create(authentication, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
    }

    /**
     * Lists tickets visible to the caller, filtered and paginated per role.
     *
     * <p>At most one assignment filter may be supplied, and only a manager may
     * filter by another assignee.
     *
     * @param authentication authenticated account identity
     * @param page zero-based page index
     * @param size page size
     * @param status optional status filter
     * @param priority optional priority filter
     * @param assignedToId optional assignee filter (manager only)
     * @param assignedToMe optional filter for the caller's own assignments
     * @param unassigned optional filter for unassigned tickets
     * @return a page of tickets the caller may view
     */
    @GetMapping
    public PageResponse<TicketResponse> list(Authentication authentication,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) TicketStatus status,
                                             @RequestParam(required = false) TicketPriority priority,
                                             @RequestParam(required = false) Integer assignedToId,
                                             @RequestParam(required = false) Boolean assignedToMe,
                                             @RequestParam(required = false) Boolean unassigned) {
        return ticketService.list(authentication, page, size, status, priority, assignedToId, assignedToMe, unassigned);
    }

    /**
     * Returns a single ticket the caller may view.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @return the requested ticket
     */
    @GetMapping("/{id}")
    public TicketResponse get(Authentication authentication, @PathVariable int id) {
        return ticketService.get(authentication, id);
    }

    /**
     * Applies an optimistically versioned edit to the caller's own open, unassigned ticket.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request editable fields and the expected version
     * @return the updated ticket
     */
    @PatchMapping("/{id}")
    public TicketResponse update(Authentication authentication, @PathVariable int id,
                                 @RequestBody UpdateTicketRequest request) {
        return ticketService.update(authentication, id, request);
    }

    /**
     * Self-assigns an open, unassigned ticket to the calling support agent and starts progress.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @return the taken ticket
     */
    @PostMapping("/{id}/take")
    public TicketResponse take(Authentication authentication, @PathVariable int id) {
        return ticketService.take(authentication, id);
    }

    /**
     * Assigns a ticket to an active technician or manager; manager only.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request target assignee
     * @return the assigned ticket
     */
    @PostMapping("/{id}/assign")
    public TicketResponse assign(Authentication authentication, @PathVariable int id,
                                 @Valid @RequestBody AssignTicketRequest request) {
        return ticketService.assign(authentication, id, request);
    }

    /**
     * Advances an assigned ticket to in-progress; only the assignee or a manager may do so.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request requested status
     * @return the updated ticket
     */
    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(Authentication authentication, @PathVariable int id,
                                       @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(authentication, id, request);
    }

    /**
     * Changes a ticket's priority; support roles only.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request new priority
     * @return the updated ticket
     */
    @PatchMapping("/{id}/priority")
    public TicketResponse changePriority(Authentication authentication, @PathVariable int id,
                                         @Valid @RequestBody ChangePriorityRequest request) {
        return ticketService.changePriority(authentication, id, request);
    }

    /**
     * Cancels an open or in-progress ticket owned by the caller, or any such ticket for a manager.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @return the cancelled ticket
     */
    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(Authentication authentication, @PathVariable int id) {
        return ticketService.cancel(authentication, id);
    }

    /**
     * Reopens a resolved ticket, clearing its assignment and resolution.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @return the reopened ticket
     */
    @PostMapping("/{id}/reopen")
    public TicketResponse reopen(Authentication authentication, @PathVariable int id) {
        return ticketService.reopen(authentication, id);
    }

    /**
     * Resolves an in-progress ticket; only the assignee or a manager may do so.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request resolution note
     * @return the resolved ticket
     */
    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(Authentication authentication, @PathVariable int id,
                                  @Valid @RequestBody ResolveTicketRequest request) {
        return ticketService.resolve(authentication, id, request);
    }

    /**
     * Lists a ticket's messages; employees receive only public comments.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param page zero-based page index
     * @param size page size
     * @return a page of visible messages ordered oldest first
     */
    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> messages(Authentication authentication, @PathVariable int id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return ticketService.listMessages(authentication, id, page, size);
    }

    /**
     * Adds a message to a ticket; employees may not add internal notes.
     *
     * @param authentication authenticated account identity
     * @param id ticket identifier
     * @param request message content and visibility type
     * @return 201 response with the created message and its location
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> addMessage(Authentication authentication, @PathVariable int id,
                                                       @Valid @RequestBody CreateMessageRequest request) {
        var created = ticketService.addMessage(authentication, id, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + id + "/messages/" + created.id())).body(created);
    }
}
