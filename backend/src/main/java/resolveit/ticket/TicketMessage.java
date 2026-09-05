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
import jakarta.persistence.Table;
import java.time.Instant;
import resolveit.user.User;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(nullable = false, length = 5_000)
    private String message;

    @Column(name = "created_at", nullable = false, length = 30)
    private Instant createdAt;

    protected TicketMessage() {}

    public TicketMessage(Ticket ticket, User author, MessageType messageType, String message) {
        this.ticket = ticket;
        this.author = author;
        this.messageType = messageType;
        this.message = message;
    }

    @PrePersist
    void created() {
        createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public User getAuthor() { return author; }
    public MessageType getMessageType() { return messageType; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
