package resolveit.ticket;

import java.util.Locale;

/** Derives a URL-safe slug from a ticket subject. */
public final class TicketSlug {
    private TicketSlug() {}

    public static String from(String subject) {
        String lower = subject.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
