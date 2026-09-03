package resolveit.ticket;

import static resolveit.ticket.TicketDtos.*;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resolveit.common.ApiException;
import resolveit.user.Role;
import resolveit.user.User;
import resolveit.user.UserRepository;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final TicketMessageRepository messages;
    private final UserRepository users;

    public TicketService(TicketRepository tickets, TicketMessageRepository messages, UserRepository users) {
        this.tickets = tickets;
        this.messages = messages;
        this.users = users;
    }

    @Transactional
    public TicketResponse create(Authentication authentication, CreateTicketRequest request) {
        var currentUser = currentUser(authentication);
        var ticket = new Ticket(newTicketNumber(), normalizeSubject(request.subject()),
                normalizeDescription(request.description()), request.category(), request.priority(), currentUser);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(Authentication authentication, int page, int size,
                                             TicketStatus status, TicketPriority priority,
                                             Integer assignedToId, Boolean assignedToMe, Boolean unassigned) {
        validatePagination(page, size, 100);
        var currentUser = currentUser(authentication);
        var assignmentFilters = (assignedToId == null ? 0 : 1)
                + (Boolean.TRUE.equals(assignedToMe) ? 1 : 0)
                + (Boolean.TRUE.equals(unassigned) ? 1 : 0);
        if (assignmentFilters > 1) {
            throw ApiException.badRequest("INVALID_FILTER", "Only one assignment filter may be supplied.");
        }
        if (assignedToId != null && currentUser.getRole() != Role.MANAGER) {
            throw ApiException.forbidden("ACCESS_DENIED", "You are not allowed to filter by another assignee.");
        }

        var specification = (org.springframework.data.jpa.domain.Specification<Ticket>) (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (currentUser.getRole() == Role.EMPLOYEE) {
                predicates.add(builder.equal(root.get("requester").get("id"), currentUser.getId()));
            } else if (currentUser.getRole() == Role.TECHNICIAN) {
                predicates.add(builder.or(
                        builder.equal(root.get("requester").get("id"), currentUser.getId()),
                        root.get("status").in(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                        builder.equal(root.get("assignedTo").get("id"), currentUser.getId())));
            }
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (priority != null) predicates.add(builder.equal(root.get("priority"), priority));
            if (assignedToId != null) predicates.add(builder.equal(root.get("assignedTo").get("id"), assignedToId));
            if (Boolean.TRUE.equals(assignedToMe)) {
                predicates.add(builder.equal(root.get("assignedTo").get("id"), currentUser.getId()));
            }
            if (Boolean.TRUE.equals(unassigned)) predicates.add(builder.isNull(root.get("assignedTo")));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = tickets.findAll(specification, pageable);
        return new PageResponse<>(result.map(TicketResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Authentication authentication, int id) {
        return TicketResponse.from(accessibleTicket(currentUser(authentication), id));
    }

    @Transactional
    public TicketResponse update(Authentication authentication, int id, UpdateTicketRequest request) {
        if (!request.hasVersion()) {
            throw ApiException.badRequest("VERSION_REQUIRED", "Version is required.");
        }
        if (!request.hasChanges()) {
            throw ApiException.badRequest("EMPTY_UPDATE", "At least one editable field must be supplied.");
        }
        var currentUser = currentUser(authentication);
        var ticket = tickets.findById(id).orElseThrow(TicketService::ticketNotFound);
        if (!ticket.getRequester().getId().equals(currentUser.getId())) throw ticketNotFound();
        if (ticket.getStatus() != TicketStatus.OPEN || ticket.getAssignedTo() != null) {
            throw ApiException.conflict("TICKET_NOT_EDITABLE", "Only an open, unassigned ticket can be edited.");
        }
        if (request.version() < 0 || ticket.getVersion() != request.version()) {
            throw ApiException.conflict("TICKET_VERSION_CONFLICT", "The ticket was modified by another request.");
        }
        if (request.hasSubject()) ticket.setSubject(normalizeSubject(request.subject()));
        if (request.hasDescription()) ticket.setDescription(normalizeDescription(request.description()));
        if (request.hasCategory()) ticket.setCategory(request.category());
        if (request.hasPriority()) ticket.setPriority(request.priority());
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse take(Authentication authentication, int id) {
        var currentUser = currentUser(authentication);
        requireSupport(currentUser);
        var visible = tickets.findById(id).filter(ticket -> canView(currentUser, ticket))
                .orElseThrow(TicketService::ticketNotFound);
        var changed = tickets.takeIfOpenAndUnassigned(id, currentUser, Instant.now());
        if (changed == 0) {
            throw ApiException.conflict("TICKET_NOT_AVAILABLE", "The ticket is no longer open and unassigned.");
        }
        return TicketResponse.from(tickets.findById(visible.getId()).orElseThrow(TicketService::ticketNotFound));
    }

    @Transactional
    public TicketResponse assign(Authentication authentication, int id, AssignTicketRequest request) {
        var currentUser = currentUser(authentication);
        requireManager(currentUser);
        var ticket = tickets.findById(id).orElseThrow(TicketService::ticketNotFound);
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw ApiException.conflict("TICKET_NOT_ASSIGNABLE", "Resolved or cancelled tickets cannot be assigned.");
        }
        var assignee = users.findById(request.technicianId())
                .filter(User::isActive)
                .filter(user -> user.getRole() == Role.TECHNICIAN || user.getRole() == Role.MANAGER)
                .orElseThrow(() -> ApiException.badRequest("INVALID_ASSIGNEE",
                        "Assignee must be an active technician or manager."));
        ticket.setAssignedTo(assignee);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(Authentication authentication, int id, ChangeStatusRequest request) {
        var currentUser = currentUser(authentication);
        requireSupport(currentUser);
        var ticket = accessibleTicket(currentUser, id);
        if (request.status() != TicketStatus.IN_PROGRESS || ticket.getStatus() != TicketStatus.OPEN) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "That ticket status transition is not allowed.");
        }
        if (ticket.getAssignedTo() == null) {
            throw ApiException.conflict("TICKET_UNASSIGNED", "The ticket must be assigned before work can begin.");
        }
        if (currentUser.getRole() != Role.MANAGER
                && !ticket.getAssignedTo().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("ACCESS_DENIED",
                    "Only the assigned technician can change this ticket's status.");
        }
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse changePriority(Authentication authentication, int id, ChangePriorityRequest request) {
        var currentUser = currentUser(authentication);
        requireSupport(currentUser);
        var ticket = accessibleTicket(currentUser, id);
        ticket.setPriority(request.priority());
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse cancel(Authentication authentication, int id) {
        var currentUser = currentUser(authentication);
        var ticket = tickets.findById(id).orElseThrow(TicketService::ticketNotFound);
        if (currentUser.getRole() != Role.MANAGER && !ticket.getRequester().getId().equals(currentUser.getId())) {
            throw ticketNotFound();
        }
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "Only open or in-progress tickets can be cancelled.");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse reopen(Authentication authentication, int id) {
        var currentUser = currentUser(authentication);
        var ticket = tickets.findById(id).orElseThrow(TicketService::ticketNotFound);
        if (currentUser.getRole() == Role.EMPLOYEE && !ticket.getRequester().getId().equals(currentUser.getId())) {
            throw ticketNotFound();
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Only resolved tickets can be reopened.");
        }
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedTo(null);
        ticket.setResolutionNote(null);
        ticket.setResolvedAt(null);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketResponse resolve(Authentication authentication, int id, ResolveTicketRequest request) {
        var currentUser = currentUser(authentication);
        requireSupport(currentUser);
        var ticket = accessibleTicket(currentUser, id);
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Only an in-progress ticket can be resolved.");
        }
        if (currentUser.getRole() != Role.MANAGER
                && (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(currentUser.getId()))) {
            throw ApiException.forbidden("ACCESS_DENIED", "Only the assigned technician can resolve this ticket.");
        }
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionNote(normalizeResolution(request.resolutionNote()));
        ticket.setResolvedAt(Instant.now());
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(Authentication authentication, int id, int page, int size) {
        validatePagination(page, size, 100);
        var currentUser = currentUser(authentication);
        accessibleTicket(currentUser, id);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        var result = currentUser.getRole() == Role.EMPLOYEE
                ? messages.findAllByTicketIdAndMessageType(id, MessageType.PUBLIC_COMMENT, pageable)
                : messages.findAllByTicketId(id, pageable);
        return new PageResponse<>(result.map(MessageResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public MessageResponse addMessage(Authentication authentication, int id, CreateMessageRequest request) {
        var currentUser = currentUser(authentication);
        var ticket = accessibleTicket(currentUser, id);
        if (request.messageType() == MessageType.INTERNAL_NOTE && currentUser.getRole() == Role.EMPLOYEE) {
            throw ApiException.forbidden("ACCESS_DENIED", "Employees cannot add internal notes.");
        }
        var message = new TicketMessage(ticket, currentUser, request.messageType(),
                normalizeMessage(request.message()));
        return MessageResponse.from(messages.saveAndFlush(message));
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication is required."));
    }

    private Ticket accessibleTicket(User currentUser, int id) {
        return tickets.findById(id).filter(ticket -> canView(currentUser, ticket))
                .orElseThrow(TicketService::ticketNotFound);
    }

    private static boolean canView(User user, Ticket ticket) {
        if (user.getRole() == Role.MANAGER) return true;
        if (ticket.getRequester().getId().equals(user.getId())) return true;
        return user.getRole() == Role.TECHNICIAN
                && (ticket.getStatus() == TicketStatus.OPEN
                    || ticket.getStatus() == TicketStatus.IN_PROGRESS
                    || ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId()));
    }

    private static void requireSupport(User user) {
        if (user.getRole() == Role.EMPLOYEE) {
            throw ApiException.forbidden("ACCESS_DENIED", "You are not allowed to perform this action.");
        }
    }

    private static void requireManager(User user) {
        if (user.getRole() != Role.MANAGER) {
            throw ApiException.forbidden("ACCESS_DENIED", "You are not allowed to perform this action.");
        }
    }

    private static void validatePagination(int page, int size, int maximumSize) {
        if (page < 0 || size < 1 || size > maximumSize) {
            throw ApiException.badRequest("INVALID_PAGINATION",
                    "Page must be non-negative and size must be between 1 and " + maximumSize + ".");
        }
    }

    private static String normalizeSubject(String value) {
        var normalized = value.trim();
        if (normalized.length() < 5 || normalized.length() > 200) {
            throw ApiException.badRequest("INVALID_TICKET", "Subject must contain 5 to 200 characters after trimming.");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        var normalized = value.trim();
        if (normalized.length() < 10 || normalized.length() > 10_000) {
            throw ApiException.badRequest("INVALID_TICKET",
                    "Description must contain 10 to 10000 characters after trimming.");
        }
        return normalized;
    }

    private static String normalizeResolution(String value) {
        var normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 10_000) {
            throw ApiException.badRequest("INVALID_RESOLUTION",
                    "Resolution note must contain 1 to 10000 characters after trimming.");
        }
        return normalized;
    }

    private static String normalizeMessage(String value) {
        var normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 5_000) {
            throw ApiException.badRequest("INVALID_MESSAGE",
                    "Message must contain 1 to 5000 characters after trimming.");
        }
        return normalized;
    }

    private static String newTicketNumber() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private static ApiException ticketNotFound() {
        return ApiException.notFound("TICKET_NOT_FOUND", "Ticket not found.");
    }
}
