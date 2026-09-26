package resolveit.ticket;

public final class TicketReferenceTest {
    public static void main(String[] args) {
        if (!"H00042".equals(TicketReference.code(TicketCategory.HARDWARE, 42))) {
            throw new AssertionError("unexpected code: " + TicketReference.code(TicketCategory.HARDWARE, 42));
        }
    }
}
