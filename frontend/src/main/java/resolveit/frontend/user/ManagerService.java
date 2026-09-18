package resolveit.frontend.user;

import java.util.List;
import java.util.concurrent.CompletionStage;
import resolveit.frontend.model.User;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;

/**
 * Manager-facing wrapper over {@link ManagerClient} that exposes user
 * administration and ticket assignment and validates account input before save.
 */
public final class ManagerService {
    private final ManagerClient client;

    /**
     * Creates the service over a manager client.
     *
     * @param client asynchronous manager API client
     */
    public ManagerService(ManagerClient client) { this.client = client; }
    public CompletionStage<PageResponse<User>> users(int page) { return client.users(page); }
    public CompletionStage<List<User>> technicians() { return client.technicians(); }
    public CompletionStage<Ticket> assign(int id, int technicianId) { return client.assign(id, technicianId); }

    /**
     * Creates a new user when {@code id} is {@code null}, otherwise updates the
     * existing user with that id.
     *
     * @param id user identifier, or {@code null} to create
     * @param request account details
     * @return a stage completing with the saved user
     */
    public CompletionStage<User> save(Integer id, ManagerClient.UserRequest request) {
        return id == null ? client.createUser(request) : client.updateUser(id, request);
    }

    /**
     * Validates account input before a save. On create, the password is required;
     * on update, an empty password means leave it unchanged.
     *
     * @param username proposed username
     * @param email proposed email
     * @param password proposed password (may be empty when updating)
     * @param creating whether this is a create rather than an update
     * @return an error message to show, or {@code null} when the input is valid
     */
    public static String validate(String username, String email, String password, boolean creating) {
        if (username.trim().length() < 3 || username.trim().length() > 50) {
            return "Username must contain 3–50 characters.";
        }
        if (email.trim().length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return "Enter a valid email address (at most 254 characters).";
        }
        if ((creating || !password.isEmpty())
                && (password.isBlank() || password.length() < 5 || password.length() > 100)) {
            return "Password must contain 5–100 characters.";
        }
        return null;
    }
}
