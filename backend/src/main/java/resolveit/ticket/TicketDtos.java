package resolveit.ticket;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class TicketDtos {
    private TicketDtos() {}

    public record CreateTicketRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 10_000) String description,
            @NotNull TicketCategory category,
            @NotNull TicketPriority priority) {}

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

        public String subject() { return subject; }
        public String description() { return description; }
        public TicketCategory category() { return category; }
        public TicketPriority priority() { return priority; }
        public Integer version() { return version; }
        public boolean hasSubject() { return subjectSet; }
        public boolean hasDescription() { return descriptionSet; }
        public boolean hasCategory() { return categorySet; }
        public boolean hasPriority() { return prioritySet; }
        public boolean hasVersion() { return versionSet; }
        public boolean hasChanges() { return subjectSet || descriptionSet || categorySet || prioritySet; }

        @JsonSetter(value = "subject", nulls = Nulls.FAIL)
        public void setSubject(String subject) { this.subject = subject; this.subjectSet = true; }

        @JsonSetter(value = "description", nulls = Nulls.FAIL)
        public void setDescription(String description) { this.description = description; this.descriptionSet = true; }

        @JsonSetter(value = "category", nulls = Nulls.FAIL)
        public void setCategory(TicketCategory category) { this.category = category; this.categorySet = true; }

        @JsonSetter(value = "priority", nulls = Nulls.FAIL)
        public void setPriority(TicketPriority priority) { this.priority = priority; this.prioritySet = true; }

        @JsonSetter(value = "version", nulls = Nulls.FAIL)
        public void setVersion(Integer version) { this.version = version; this.versionSet = true; }

        @JsonAnySetter
        public void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("Unknown ticket update field: " + name);
        }
    }

    public record AssignTicketRequest(@NotNull Integer technicianId) {}
    public record ChangeStatusRequest(@NotNull TicketStatus status) {}
    public record ChangePriorityRequest(@NotNull TicketPriority priority) {}
    public record ResolveTicketRequest(@NotBlank @Size(max = 10_000) String resolutionNote) {}
    public record CreateMessageRequest(
            @NotNull MessageType messageType,
            @NotBlank @Size(max = 5_000) String message) {}

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

    public record MessageResponse(
            Integer id,
            Integer ticketId,
            Integer authorId,
            String authorUsername,
            MessageType messageType,
            String message,
            Instant createdAt) {
        public static MessageResponse from(TicketMessage message) {
            return new MessageResponse(message.getId(), message.getTicket().getId(), message.getAuthor().getId(),
                    message.getAuthor().getUsername(), message.getMessageType(), message.getMessage(),
                    message.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
