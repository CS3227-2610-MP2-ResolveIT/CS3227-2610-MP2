package resolveit.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import resolveit.user.User;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 30)
    private String ticketNumber;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 10_000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "resolution_note", length = 10_000)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, length = 30)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, length = 30)
    private Instant updatedAt;

    @Column(name = "resolved_at", length = 30)
    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected Ticket() {}

    public Ticket(String ticketNumber, String subject, String description, TicketCategory category,
                  TicketPriority priority, User requester) {
        this.ticketNumber = ticketNumber;
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.requester = requester;
        this.status = TicketStatus.OPEN;
    }

    @PrePersist
    void created() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }

    public Integer getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public TicketCategory getCategory() { return category; }
    public TicketPriority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public User getRequester() { return requester; }
    public User getAssignedTo() { return assignedTo; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public int getVersion() { return version; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(TicketCategory category) { this.category = category; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
