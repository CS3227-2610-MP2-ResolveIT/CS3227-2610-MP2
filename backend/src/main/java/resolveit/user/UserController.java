package resolveit.user;

import static resolveit.user.UserDtos.CreateUserRequest;
import static resolveit.user.UserDtos.PageResponse;
import static resolveit.user.UserDtos.UpdateUserRequest;
import static resolveit.user.UserDtos.UserResponse;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes user administration and the technician-directory lookup. */
@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    /**
     * Creates the user controller.
     *
     * @param userService user administration service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lists users with optional role filtering and pagination.
     *
     * @param page zero-based page index
     * @param size page size
     * @param role optional role filter
     * @return a page of users
     */
    @GetMapping("/users")
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) Role role) {
        return userService.list(page, size, role);
    }

    /**
     * Creates a user; the username and email must be unique.
     *
     * @param request validated user details
     * @return 201 response with the created user and its location
     */
    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        var created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    /**
     * Applies a partial update to a user; supplied username and email must stay unique.
     *
     * @param id user identifier
     * @param request fields to change
     * @return the updated user
     */
    @PatchMapping("/users/{id}")
    public UserResponse update(@PathVariable int id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    /**
     * Lists active technicians and managers available as ticket assignees.
     *
     * @return active assignable support users ordered by username
     */
    @GetMapping("/technicians")
    public List<UserResponse> technicians() {
        return userService.technicians();
    }
}
