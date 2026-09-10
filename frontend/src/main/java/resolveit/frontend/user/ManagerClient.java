package resolveit.frontend.user;

import java.util.List;
import java.util.concurrent.CompletionStage;
import resolveit.frontend.model.User;
import resolveit.frontend.model.Role;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;

public interface ManagerClient {
    CompletionStage<PageResponse<User>> users(int page);
    CompletionStage<List<User>> technicians();
    CompletionStage<User> createUser(UserRequest request);
    CompletionStage<User> updateUser(int id, UserRequest request);
    CompletionStage<Ticket> assign(int id, int technicianId);
    record UserRequest(String username, String email, String password, Role role, boolean active) {}
}
