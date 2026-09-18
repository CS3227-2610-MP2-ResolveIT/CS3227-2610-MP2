package resolveit.frontend.user;

import java.util.List;
import java.util.concurrent.CompletionStage;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;

/**
 * Asynchronous boundary to the backend user-administration and assignment APIs
 * used by managers. Each method completes on a background thread and fails when
 * the server rejects the request.
 */
public interface ManagerClient {
    /**
     * Lists user accounts.
     *
     * @param page zero-based page index
     * @return a stage completing with a page of users
     */
    CompletionStage<PageResponse<User>> users(int page);

    /**
     * Lists active technicians and managers available as assignees.
     *
     * @return a stage completing with the assignable support users
     */
    CompletionStage<List<User>> technicians();

    /**
     * Creates a user account.
     *
     * @param request account details
     * @return a stage completing with the created user
     */
    CompletionStage<User> createUser(UserRequest request);

    /**
     * Applies a partial update to a user account.
     *
     * @param id user identifier
     * @param request fields to change
     * @return a stage completing with the updated user
     */
    CompletionStage<User> updateUser(int id, UserRequest request);

    /**
     * Assigns a ticket to a technician.
     *
     * @param id ticket identifier
     * @param technicianId assignee identifier
     * @return a stage completing with the assigned ticket
     */
    CompletionStage<Ticket> assign(int id, int technicianId);

    /**
     * User account details for create and update requests.
     *
     * @param username account username
     * @param email account email
     * @param password new password, or empty to leave an existing one unchanged
     * @param role account role
     * @param active whether the account is active
     */
    record UserRequest(String username, String email, String password, Role role, boolean active) {}
}
