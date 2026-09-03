package resolveit.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Integer> {
    Page<TicketMessage> findAllByTicketId(int ticketId, Pageable pageable);
    Page<TicketMessage> findAllByTicketIdAndMessageType(int ticketId, MessageType messageType, Pageable pageable);
}
