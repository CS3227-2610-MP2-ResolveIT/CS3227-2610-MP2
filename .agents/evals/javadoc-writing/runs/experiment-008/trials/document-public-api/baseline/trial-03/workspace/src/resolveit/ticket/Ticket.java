package resolveit.ticket;

/**
 * Describes a ticket and whether it contributes to the daily ticket quota.
 *
 * @param id the unique identifier of the ticket
 * @param subject the subject of the ticket
 * @param countsAgainstQuota whether this ticket counts toward the daily quota
 */
public record Ticket(long id, String subject, boolean countsAgainstQuota) {
}
