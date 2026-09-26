package resolveit.ticket;

public final class TicketSlugTest {
    public static void main(String[] args) {
        if (!"vpn-unavailable".equals(TicketSlug.from("VPN Unavailable!"))) {
            throw new AssertionError("unexpected slug: " + TicketSlug.from("VPN Unavailable!"));
        }
    }
}
