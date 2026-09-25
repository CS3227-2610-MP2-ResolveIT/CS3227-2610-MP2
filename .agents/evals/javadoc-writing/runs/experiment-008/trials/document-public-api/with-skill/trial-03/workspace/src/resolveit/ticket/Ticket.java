package resolveit.ticket;

/**
 * Represents a support ticket and whether it consumes a daily quota slot.
 *
 * @param id the ticket's unique identifier
 * @param subject the ticket's subject
 * @param countsAgainstQuota whether the ticket consumes a daily quota slot
 */
public record Ticket(long id, String subject, boolean countsAgainstQuota) {
}
