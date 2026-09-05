package resolveit.ticket;

import static resolveit.ticket.TicketDtos.*;

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

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(Authentication authentication,
                                                  @Valid @RequestBody CreateTicketRequest request) {
        var created = ticketService.create(authentication, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
    }

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

    @GetMapping("/{id}")
    public TicketResponse get(Authentication authentication, @PathVariable int id) {
        return ticketService.get(authentication, id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(Authentication authentication, @PathVariable int id,
                                 @RequestBody UpdateTicketRequest request) {
        return ticketService.update(authentication, id, request);
    }

    @PostMapping("/{id}/take")
    public TicketResponse take(Authentication authentication, @PathVariable int id) {
        return ticketService.take(authentication, id);
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(Authentication authentication, @PathVariable int id,
                                 @Valid @RequestBody AssignTicketRequest request) {
        return ticketService.assign(authentication, id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(Authentication authentication, @PathVariable int id,
                                       @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(authentication, id, request);
    }

    @PatchMapping("/{id}/priority")
    public TicketResponse changePriority(Authentication authentication, @PathVariable int id,
                                         @Valid @RequestBody ChangePriorityRequest request) {
        return ticketService.changePriority(authentication, id, request);
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(Authentication authentication, @PathVariable int id) {
        return ticketService.cancel(authentication, id);
    }

    @PostMapping("/{id}/reopen")
    public TicketResponse reopen(Authentication authentication, @PathVariable int id) {
        return ticketService.reopen(authentication, id);
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(Authentication authentication, @PathVariable int id,
                                  @Valid @RequestBody ResolveTicketRequest request) {
        return ticketService.resolve(authentication, id, request);
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> messages(Authentication authentication, @PathVariable int id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return ticketService.listMessages(authentication, id, page, size);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> addMessage(Authentication authentication, @PathVariable int id,
                                                       @Valid @RequestBody CreateMessageRequest request) {
        var created = ticketService.addMessage(authentication, id, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + id + "/messages/" + created.id())).body(created);
    }
}
