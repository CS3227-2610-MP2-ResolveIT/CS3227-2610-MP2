package resolveit.frontend.model;

public record User(
        int id,
        String username,
        String email,
        Role role,
        boolean active,
        String createdAt,
        String updatedAt) {
}
