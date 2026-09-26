package resolveit.ticket;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** JSON request and response contracts for the ticket REST API. */
public final class TicketDtos {
    private TicketDtos() {}

    /**
     * Request body for creating a new unassigned open ticket.
     *
     * @param subject ticket subject
     * @param description ticket description
     * @param category fixed ticket category
     * @param priority ticket priority
     */
    public record CreateTicketRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 10_000) String description,
            @NotNull TicketCategory category,
            @NotNull TicketPriority priority) {}

    /**
     * Request body for a partial ticket update with explicit-field tracking.
     * Omitted fields remain unchanged, while explicit null and unknown fields
     * are rejected before the service applies the update.
     */
    public static final class UpdateTicketRequest {
        private String subject;
        private String description;
        private TicketCategory category;
        private TicketPriority priority;
        private Integer version;
        private boolean subjectSet;
        private boolean descriptionSet;
        private boolean categorySet;
        private boolean prioritySet;
        private boolean versionSet;

        /**
         * Returns the supplied subject, or null when it was omitted.
         *
         * @return supplied subject, or null when omitted
         */
        public String subject() { return subject; }

        /**
         * Returns the supplied description, or null when it was omitted.
         *
         * @return supplied description, or null when omitted
         */
        public String description() { return description; }

        /**
         * Returns the supplied category, or null when it was omitted.
         *
         * @return supplied category, or null when omitted
         */
        public TicketCategory category() { return category; }

        /**
         * Returns the supplied priority, or null when it was omitted.
         *
         * @return supplied priority, or null when omitted
         */
        public TicketPriority priority() { return priority; }

        /**
         * Returns the client version used for optimistic locking.
         *
         * @return client version, or null when omitted
         */
        public Integer version() { return version; }

        /** @return whether the request supplied a subject. */
        public boolean hasSubject() { return subjectSet; }

        /** @return whether the request supplied a description. */
        public boolean hasDescription() { return descriptionSet; }

        /** @return whether the request supplied a category. */
        public boolean hasCategory() { return categorySet; }

        /** @return whether the request supplied a priority. */
        public boolean hasPriority() { return prioritySet; }

        /** @return whether the request supplied a version. */
        public boolean hasVersion() { return versionSet; }

        /** @return whether at least one editable field was supplied. */
        public boolean hasChanges() { return subjectSet || descriptionSet || categorySet || prioritySet; }

        /**
         * Records that the subject was supplied and stores its value.
         *
         * @param subject supplied subject
         */
        @JsonSetter(value = "subject", nulls = Nulls.FAIL)
        public void setSubject(String subject) { this.subject = subject; this.subjectSet = true; }

        /**
         * Records that the description was supplied and stores its value.
         *
         * @param description supplied description
         */
        @JsonSetter(value = "description", nulls = Nulls.FAIL)
        public void setDescription(String description) { this.description = description; this.descriptionSet = true; }

        /**
         * Records that the category was supplied and stores its value.
         *
         * @param category supplied category
         */
        @JsonSetter(value = "category", nulls = Nulls.FAIL)
        public void setCategory(TicketCategory category) { this.category = category; this.categorySet = true; }

        /**
         * Records that the priority was supplied and stores its value.
         *
         * @param priority supplied priority
         */
        @JsonSetter(value = "priority", nulls = Nulls.FAIL)
        public void setPriority(TicketPriority priority) { this.priority = priority; this.prioritySet = true; }

        /**
         * Records the client version required for optimistic locking.
         *
         * @param version client version
         */
        @JsonSetter(value = "version", nulls = Nulls.FAIL)
        public void setVersion(Integer version) { this.version = version; this.versionSet = true; }

        /**
         * Rejects fields outside the supported partial-update contract.
         *
         * @param name unknown JSON field name
         * @param value unknown JSON field value
         */
        @JsonAnySetter
        public void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("Unknown ticket update field: " + name);
        }
    }

    /**
     * Request body for assigning a ticket to an active support user.
     *
     * @param technicianId target support-user identifier
     */
    public record AssignTicketRequest(@NotNull Integer technicianId) {}

    /**
     * Request body for a permitted ticket status transition.
     *
     * @param status target ticket status
     */
    public record ChangeStatusRequest(@NotNull TicketStatus status) {}

    /**
     * Request body for changing ticket priority.
     *
     * @param priority new ticket priority
     */
    public record ChangePriorityRequest(@NotNull TicketPriority priority) {}

    /**
     * Request body for resolving a ticket with a non-blank resolution note.
     *
     * @param resolutionNote explanation of the completed resolution
     */
    public record ResolveTicketRequest(@NotBlank @Size(max = 10_000) String resolutionNote) {}

    /**
     * Request body for adding a public comment or internal note.
     *
     * @param messageType visibility of the message
     * @param message message body
     */
    public record CreateMessageRequest(
            @NotNull MessageType messageType,
            @NotBlank @Size(max = 5_000) String message) {}

    /**
     * API representation of a ticket, including its workflow and assignment state.
     *
     * @param id database identifier
     * @param ticketNumber public ticket number
     * @param subject ticket subject
     * @param description ticket description
     * @param category fixed ticket category
     * @param priority ticket priority
     * @param status current workflow status
     * @param requesterId requester identifier
     * @param requesterUsername requester username
     * @param assignedToId assignee identifier, or null when unassigned
     * @param assignedToUsername assignee username, or null when unassigned
     * @param resolutionNote resolution text, or null when unresolved
     * @param createdAt creation timestamp
     * @param updatedAt last-update timestamp
     * @param resolvedAt resolution timestamp, or null when unresolved
     * @param version optimistic-lock version
     */
    public record TicketResponse(
            Integer id,
            String ticketNumber,
            String subject,
            String description,
            TicketCategory category,
            TicketPriority priority,
            TicketStatus status,
            Integer requesterId,
            String requesterUsername,
            Integer assignedToId,
            String assignedToUsername,
            String resolutionNote,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt,
            int version) {
        /**
         * Maps the persistence entity to a response without exposing the entity itself.
         *
         * @param ticket persistence entity
         * @return safe API response
         */
        public static TicketResponse from(Ticket ticket) {
            var assignee = ticket.getAssignedTo();
            return new TicketResponse(ticket.getId(), ticket.getTicketNumber(), ticket.getSubject(),
                    ticket.getDescription(), ticket.getCategory(), ticket.getPriority(), ticket.getStatus(),
                    ticket.getRequester().getId(), ticket.getRequester().getUsername(),
                    assignee == null ? null : assignee.getId(), assignee == null ? null : assignee.getUsername(),
                    ticket.getResolutionNote(), ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt(),
                    ticket.getVersion());
        }
    }

    /**
     * API representation of a ticket message and its author.
     *
     * @param id message identifier
     * @param ticketId parent ticket identifier
     * @param authorId author identifier
     * @param authorUsername author username
     * @param messageType message visibility type
     * @param message message body
     * @param createdAt creation timestamp
     */
    public record MessageResponse(
            Integer id,
            Integer ticketId,
            Integer authorId,
            String authorUsername,
            MessageType messageType,
            String message,
            Instant createdAt) {
        /**
         * Maps the persistence message to a response.
         *
         * @param message persistence message
         * @return safe API response
         */
        public static MessageResponse from(TicketMessage message) {
            return new MessageResponse(message.getId(), message.getTicket().getId(), message.getAuthor().getId(),
                    message.getAuthor().getUsername(), message.getMessageType(), message.getMessage(),
                    message.getCreatedAt());
        }
    }

    /**
     * Paginated API response with stable page metadata.
     *
     * @param <T> element type
     * @param content elements on the current page
     * @param page zero-based page index
     * @param size requested page size
     * @param totalElements total matching elements
     * @param totalPages total available pages
     */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
