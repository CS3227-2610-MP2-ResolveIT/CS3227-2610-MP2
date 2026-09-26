package resolveit.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    /**
     * Request body for creating a user account.
     *
     * @param username unique display/login name
     * @param email unique email address
     * @param password initial plaintext password supplied over the local API
     * @param role assigned account role
     * @param active initial activation state, or null for the service default
     */
    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 5, max = 100) String password,
            @NotNull Role role,
            Boolean active) {}

    /**
     * Request body for a partial user-account update.
     *
     * @param username replacement username, or null when unchanged
     * @param email replacement email, or null when unchanged
     * @param password replacement password, or null when unchanged
     * @param role replacement role, or null when unchanged
     * @param active replacement activation state, or null when unchanged
     */
    public record UpdateUserRequest(
            @Size(min = 3, max = 50) String username,
            @Email @Size(max = 254) String email,
            @Size(min = 5, max = 100) String password,
            Role role,
            Boolean active) {
        /**
         * Reports whether the request contains no account changes.
         *
         * @return true when every update field is null
         */
        public boolean isEmpty() {
            return username == null && email == null && password == null && role == null && active == null;
        }
    }

    /**
     * Safe API representation of a user account without password data.
     *
     * @param id account identifier
     * @param username account username
     * @param email account email
     * @param role account role
     * @param active whether the account is active
     * @param createdAt creation timestamp
     * @param updatedAt last-update timestamp
     */
    public record UserResponse(
            Integer id,
            String username,
            String email,
            Role role,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        /** Maps a persistence user to a response safe for API clients. */
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                    user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    /**
     * Paginated API response with stable page metadata.
     *
     * @param <T> element type
     * @param content elements on the current page
     * @param page zero-based page index
     * @param size requested page size
     * @param totalElements total matching elements
     * @param totalPages total available pages
     */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
