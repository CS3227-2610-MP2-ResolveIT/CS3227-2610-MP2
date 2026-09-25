package resolveit.ticket;

/**
 * Represents a ticket and whether it contributes to the daily quota.
 *
 * @param id the ticket's unique identifier
 * @param subject the ticket's subject
 * @param countsAgainstQuota whether the ticket contributes to the daily quota
 */
public record Ticket(
        /** The ticket's unique identifier. */
        long id,
        /** The ticket's subject. */
        String subject,
        /** Whether the ticket contributes to the daily quota. */
        boolean countsAgainstQuota) {
}
