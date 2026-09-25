package resolveit.ticket;

/**
 * Describes a ticket and whether it contributes to the daily ticket quota.
 *
 * @param id the ticket's unique identifier
 * @param subject the ticket subject
 * @param countsAgainstQuota whether this ticket consumes one unit of the
 *                           daily quota
 */
public record Ticket(long id, String subject, boolean countsAgainstQuota) {
}
