package resolveit.frontend.ticket;

public enum TicketPriority {
    LOW("Low"), MEDIUM("Medium"), HIGH("High");

    private final String displayName;

    TicketPriority(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

}
