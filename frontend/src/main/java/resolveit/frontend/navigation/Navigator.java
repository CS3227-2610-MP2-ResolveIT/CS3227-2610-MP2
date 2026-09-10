package resolveit.frontend.navigation;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import resolveit.frontend.auth.AuthService;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.EmployeeTicketService;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ui.AuthenticatedController;
import resolveit.frontend.ui.LoginController;
import resolveit.frontend.ui.TechnicianController;
import resolveit.frontend.model.Role;

public final class Navigator {
    private static final String STYLESHEET = "/resolveit/frontend/styles/app.css";

    private final Stage stage;
    private final AuthService authService;
    private final EmployeeTicketService ticketService;
    private final TechnicianTicketService technicianTicketService;
    private final SessionState session;
    private resolveit.frontend.user.ManagerService managerService;
    private ViewLifecycle activeController;

    public void setManagerService(resolveit.frontend.user.ManagerService service) { this.managerService = service; }
    public resolveit.frontend.user.ManagerService managerService() { return managerService; }

    public void showUsers() {
        if (session.current().orElseThrow().user().role() != Role.MANAGER) return;
        show("/resolveit/frontend/views/users.fxml", type -> new resolveit.frontend.ui.UsersController(session, managerService, this));
    }

    public Navigator(Stage stage, AuthService authService, EmployeeTicketService ticketService,
                     TechnicianTicketService technicianTicketService, SessionState session) {
        this.stage = stage;
        this.authService = authService;
        this.ticketService = ticketService;
        this.technicianTicketService = technicianTicketService;
        this.session = session;
    }

    public void showLogin() {
        show("/resolveit/frontend/views/login.fxml", type -> {
            if (type == LoginController.class) {
                return new LoginController(authService, this);
            }
            throw new IllegalArgumentException("Unsupported FXML controller: " + type.getName());
        });
    }

    public void showAuthenticated() {
        if (session.current().isEmpty()) {
            showLogin();
            return;
        }
        if (session.current().orElseThrow().user().role() != Role.EMPLOYEE) {
            show("/resolveit/frontend/views/technician.fxml", type -> {
                if (type == TechnicianController.class) {
                    return new TechnicianController(session, technicianTicketService, this);
                }
                throw new IllegalArgumentException("Unsupported FXML controller: " + type.getName());
            });
            return;
        }
        showRequesterWorkspace();
    }

    public void showRequesterWorkspace() {
        show("/resolveit/frontend/views/authenticated.fxml", type -> {
            if (type == AuthenticatedController.class) {
                return new AuthenticatedController(session, ticketService, this);
            }
            throw new IllegalArgumentException("Unsupported FXML controller: " + type.getName());
        });
    }

    private void show(String viewPath, javafx.util.Callback<Class<?>, Object> controllerFactory) {
        try {
            var loader = new FXMLLoader(requiredResource(viewPath));
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();
            var nextController = (ViewLifecycle) loader.getController();

            if (activeController != null) {
                activeController.dispose();
            }
            var scene = new Scene(root, 1120, 720);
            scene.getStylesheets().add(requiredResource(STYLESHEET).toExternalForm());
            stage.setScene(scene);
            activeController = nextController;
            activeController.onShown();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view " + viewPath, exception);
        }
    }

    private URL requiredResource(String path) {
        var resource = Navigator.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Missing application resource: " + path);
        }
        return resource;
    }
}
