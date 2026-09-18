package resolveit.frontend.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import resolveit.frontend.model.Role;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.user.ManagerService;

/** Coordinates the technician workspace shell and its queue and ticket-detail child controllers. */
public final class TechnicianController implements ViewLifecycle {
    private final SessionState session;
    private final Navigator navigator;

    @FXML private Label workspaceLabel, userNameLabel, userRoleLabel, avatarLabel;
    @FXML private Button usersNavButton, requestsNavButton, queueNavButton, logoutButton;
    @FXML private StackPane queuePage, detailPage;
    @FXML private TechnicianQueueController queuePageController;
    @FXML private TechnicianTicketDetailController detailPageController;

    /**
     * Creates the workspace shell that coordinates the queue and detail child views.
     *
     * @param session authenticated user session used to populate the workspace header
     * @param ticketService technician operations supplied to FXML child controllers
     * @param managerService manager operations supplied to the ticket-detail child controller
     * @param navigator application navigator used for view changes and sign-out
     */
    public TechnicianController(SessionState session, TechnicianTicketService ticketService,
                                ManagerService managerService, Navigator navigator) {
        this.session = session;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        var user = session.current().orElseThrow().user();
        var manager = user.role() == Role.MANAGER;
        userNameLabel.setText(user.username());
        userRoleLabel.setText(user.role().displayName());
        avatarLabel.setText(initials(user.username()));
        usersNavButton.setVisible(manager);
        usersNavButton.setManaged(manager);
        if (manager) {
            workspaceLabel.setText("Manager workspace");
            queueNavButton.setText("All _Tickets");
        }
        queuePageController.setTicketSelectedHandler(this::showTicket);
        queuePageController.setFailureHandler(this::handleQueueFailure);
        detailPageController.setQueueHandler(this::showQueue);
        detailPageController.setQueueRefreshHandler(queuePageController::refreshInBackground);
        detailPageController.setBusyHandler(this::setDetailNavigationDisabled);
    }

    @Override
    public void onShown() {
        showQueue();
    }

    @FXML
    private void showQueue() {
        showPage(queuePage);
        queuePageController.showQueue();
        setQueueNavigationActive();
    }

    private void showTicket(int ticketId) {
        showPage(detailPage);
        detailPageController.openTicket(ticketId);
        setQueueNavigationActive();
    }

    private void handleQueueFailure(Throwable failure) {
        if (failure instanceof TicketFailure ticketFailure
                && ticketFailure.kind() == TicketFailure.Kind.UNAUTHORIZED) {
            session.clear();
            navigator.showLogin();
        }
    }

    private void setDetailNavigationDisabled(boolean disabled) {
        queueNavButton.setDisable(disabled);
        usersNavButton.setDisable(disabled);
        requestsNavButton.setDisable(disabled);
        queuePageController.setDetailBusy(disabled);
    }

    private void showPage(StackPane page) {
        queuePage.setVisible(page == queuePage);
        queuePage.setManaged(page == queuePage);
        detailPage.setVisible(page == detailPage);
        detailPage.setManaged(page == detailPage);
    }

    @FXML
    private void showRequests() {
        navigator.showRequesterWorkspace();
    }

    @FXML
    private void showUsers() {
        navigator.showUsers();
    }

    @FXML
    private void logout() {
        if (!logoutButton.isDisabled()) {
            logoutButton.setDisable(true);
            navigator.signOut();
        }
    }

    @Override
    public void dispose() {
        queuePageController.dispose();
        detailPageController.dispose();
    }

    private void setQueueNavigationActive() {
        if (!queueNavButton.getStyleClass().contains("nav-button-active")) {
            queueNavButton.getStyleClass().add("nav-button-active");
        }
    }

    private static String initials(String username) {
        if (username == null || username.isBlank()) {
            return "RI";
        }
        var parts = username.trim().split("[._\\-\\s]+", 2);
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
