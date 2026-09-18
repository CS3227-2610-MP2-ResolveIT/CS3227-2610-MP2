package resolveit.ticket;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import resolveit.user.User;

/** Persists tickets and supports dynamic, specification-based queries. */
public interface TicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {
    /**
     * Atomically assigns a ticket and moves it to in-progress only while it is
     * still open and unassigned, bumping the optimistic version.
     *
     * @param ticketId ticket identifier
     * @param assignee support agent to assign
     * @param now assignment timestamp
     * @return the number of rows updated: {@code 1} when taken, {@code 0} when
     *     the ticket was already assigned or no longer open
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Ticket ticket
               set ticket.assignedTo = :assignee,
                   ticket.status = resolveit.ticket.TicketStatus.IN_PROGRESS,
                   ticket.updatedAt = :now,
                   ticket.version = ticket.version + 1
             where ticket.id = :ticketId
               and ticket.assignedTo is null
               and ticket.status = resolveit.ticket.TicketStatus.OPEN
            """)
    int takeIfOpenAndUnassigned(@Param("ticketId") int ticketId,
                                @Param("assignee") User assignee,
                                @Param("now") Instant now);
}
