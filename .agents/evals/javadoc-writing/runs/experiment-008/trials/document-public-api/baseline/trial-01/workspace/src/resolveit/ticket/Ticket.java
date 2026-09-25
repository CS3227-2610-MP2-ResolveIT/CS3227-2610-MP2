package resolveit.ticket;

/**
 * Represents a ticket and whether it contributes to the daily ticket quota.
 *
 * @param id the ticket's identifier
 * @param subject the ticket's subject
 * @param countsAgainstQuota whether the ticket counts against the daily quota
 */
public record Ticket(long id, String subject, boolean countsAgainstQuota) {
}
