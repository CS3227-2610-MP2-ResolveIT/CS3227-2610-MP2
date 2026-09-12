package resolveit.frontend.user;

import java.util.List;
import java.util.concurrent.CompletionStage;
import resolveit.frontend.model.User;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;

public final class ManagerService {
    private final ManagerClient client;
    public ManagerService(ManagerClient client) { this.client = client; }
    public CompletionStage<PageResponse<User>> users(int page) { return client.users(page); }
    public CompletionStage<List<User>> technicians() { return client.technicians(); }
    public CompletionStage<Ticket> assign(int id, int technicianId) { return client.assign(id, technicianId); }
    public CompletionStage<User> save(Integer id, ManagerClient.UserRequest request) {
        return id == null ? client.createUser(request) : client.updateUser(id, request);
    }
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
