package resolveit.frontend.model;

public enum Role {
    EMPLOYEE("Employee"),
    TECHNICIAN("IT Technician"),
    MANAGER("IT Manager");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
