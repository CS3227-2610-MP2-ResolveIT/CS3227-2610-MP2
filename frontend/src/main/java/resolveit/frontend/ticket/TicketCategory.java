package resolveit.frontend.ticket;

public enum TicketCategory {
    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    NETWORK("Network"),
    ACCOUNT_ACCESS("Account access"),
    OTHER("Other");

    private final String displayName;

    TicketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

}
