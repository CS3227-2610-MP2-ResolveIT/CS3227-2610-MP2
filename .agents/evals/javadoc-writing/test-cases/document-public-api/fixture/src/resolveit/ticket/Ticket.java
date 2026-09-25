package resolveit.ticket;

public record Ticket(long id, String subject, boolean countsAgainstQuota) {
}
