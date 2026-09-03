package resolveit.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 5, max = 100) String password,
            @NotNull Role role,
            Boolean active) {}

    public record UpdateUserRequest(
            @Size(min = 3, max = 50) String username,
            @Email @Size(max = 254) String email,
            @Size(min = 5, max = 100) String password,
            Role role,
            Boolean active) {
        public boolean isEmpty() {
            return username == null && email == null && password == null && role == null && active == null;
        }
    }

    public record UserResponse(
            Integer id,
            String username,
            String email,
            Role role,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                    user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
