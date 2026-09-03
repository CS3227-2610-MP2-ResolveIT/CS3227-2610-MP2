package resolveit.user;

import static resolveit.user.UserDtos.*;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resolveit.common.ApiException;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size, Role role) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("INVALID_PAGINATION", "Page must be non-negative and size must be between 1 and 100.");
        }
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var result = role == null ? users.findAll(pageable) : users.findAllByRole(role, pageable);
        return new PageResponse<>(result.map(UserResponse::from).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> technicians() {
        return users.findAllByActiveTrueAndRoleInOrderByUsernameAsc(List.of(Role.TECHNICIAN, Role.MANAGER))
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        var username = normalizeUsername(request.username());
        var email = normalizeEmail(request.email());
        ensureUnique(username, email, null);
        var user = new User(username, email, passwordEncoder.encode(request.password()), request.role(),
                request.active() == null || request.active());
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("USER_ALREADY_EXISTS", "A user with that username or email already exists.");
        }
    }

    @Transactional
    public UserResponse update(int id, UpdateUserRequest request) {
        if (request.isEmpty()) {
            throw ApiException.badRequest("EMPTY_UPDATE", "At least one field must be supplied.");
        }
        var user = users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
        var username = request.username() == null ? user.getUsername() : normalizeUsername(request.username());
        var email = request.email() == null ? user.getEmail() : normalizeEmail(request.email());
        ensureUnique(username, email, id);
        user.setUsername(username);
        user.setEmail(email);
        if (request.password() != null) user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.role() != null) user.setRole(request.role());
        if (request.active() != null) user.setActive(request.active());
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("USER_ALREADY_EXISTS", "A user with that username or email already exists.");
        }
    }

    private void ensureUnique(String username, String email, Integer currentId) {
        users.findAll().stream()
                .filter(user -> !user.getId().equals(currentId))
                .filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(email))
                .findAny()
                .ifPresent(user -> { throw ApiException.conflict("USER_ALREADY_EXISTS", "A user with that username or email already exists."); });
    }

    private static String normalizeUsername(String username) {
        var normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 50) {
            throw ApiException.badRequest("INVALID_USER", "Username must contain 3 to 50 characters after trimming.");
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
