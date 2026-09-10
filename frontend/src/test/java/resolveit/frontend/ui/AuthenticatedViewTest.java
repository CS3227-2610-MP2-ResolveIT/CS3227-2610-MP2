package resolveit.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.AuthService;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.EmployeeTicketService;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketClient;
import resolveit.frontend.ticket.TicketMessage;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;
import resolveit.frontend.ticket.TicketStatus;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;

class AuthenticatedViewTest {
    @Test
    void loadsRoleWorkspacesAndTheirPrimaryControls() throws Exception {
        var finished = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();
        Platform.startup(() -> Platform.runLater(() -> {
            try {
                var session = employeeSession();
                var ticketService = new EmployeeTicketService(new EmptyTicketClient());
                var authService = new AuthService(request -> CompletableFuture.failedFuture(
                        new UnsupportedOperationException()), session);
                var navigator = new Navigator(new Stage(), authService, ticketService,
                        new TechnicianTicketService(new EmptyTicketClient()), session);
                var loader = new FXMLLoader(getClass().getResource(
                        "/resolveit/frontend/views/authenticated.fxml"));
                loader.setControllerFactory(type -> new AuthenticatedController(session, ticketService, navigator));
                Parent root = loader.load();
                var scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource(
                        "/resolveit/frontend/styles/app.css").toExternalForm());
                root.applyCss();

                assertNotNull(root.lookup("#ticketsTable"));
                assertNotNull(root.lookup("#createSubjectField"));
                assertNotNull(root.lookup("#messagesList"));

                var technicianSession = technicianSession();
                var client = new EmptyTicketClient();
                var technicianService = new TechnicianTicketService(client);
                var technicianNavigator = new Navigator(new Stage(), authService,
                        new EmployeeTicketService(client), technicianService, technicianSession);
                var technicianLoader = new FXMLLoader(getClass().getResource(
                        "/resolveit/frontend/views/technician.fxml"));
                technicianLoader.setControllerFactory(type -> new TechnicianController(
                        technicianSession, technicianService, technicianNavigator));
                Parent technicianRoot = technicianLoader.load();
                var technicianScene = new Scene(technicianRoot);
                technicianScene.getStylesheets().add(getClass().getResource(
                        "/resolveit/frontend/styles/app.css").toExternalForm());
                technicianRoot.applyCss();

                assertNotNull(technicianRoot.lookup("#assignmentFilter"));
                assertNotNull(technicianRoot.lookup("#takeButton"));
                assertNotNull(technicianRoot.lookup("#resolutionField"));
                assertNotNull(technicianRoot.lookup("#messageTypeField"));
                var managerSession = new SessionState();
                managerSession.start(new LoginResponse("token", "Bearer", 900,
                        new User(3, "manager", "manager@example.test", Role.MANAGER, true, null, null)));
                var managerLoader = new FXMLLoader(getClass().getResource("/resolveit/frontend/views/technician.fxml"));
                managerLoader.setControllerFactory(type -> new TechnicianController(managerSession, technicianService, technicianNavigator));
                Parent managerRoot = managerLoader.load();
                var managerScene = new Scene(managerRoot, 1120, 720);
                managerScene.getStylesheets().add(getClass().getResource("/resolveit/frontend/styles/app.css").toExternalForm());
                managerRoot.applyCss(); managerRoot.layout();
                org.junit.jupiter.api.Assertions.assertTrue(managerRoot.lookup("#usersNavButton").isVisible());
                org.junit.jupiter.api.Assertions.assertTrue(managerRoot.lookup("#assignmentBox").isVisible());
                org.junit.jupiter.api.Assertions.assertEquals("All Tickets", ((javafx.scene.control.Label) managerRoot.lookup("#queueHeading")).getText());
                var usersLoader = new FXMLLoader(getClass().getResource("/resolveit/frontend/views/users.fxml"));
                usersLoader.setControllerFactory(type -> new UsersController(managerSession, null, technicianNavigator));
                Parent usersRoot = usersLoader.load();
                var usersScene = new Scene(usersRoot, 900, 620);
                usersScene.getStylesheets().add(getClass().getResource("/resolveit/frontend/styles/app.css").toExternalForm());
                usersRoot.applyCss(); usersRoot.layout();
                assertNotNull(usersRoot.lookup("#usersTable"));
                assertNotNull(usersRoot.lookup("#passwordField"));
                assertNotNull(usersRoot.lookup("#saveButton"));
                savePreview(managerRoot, "manager-tickets");
                savePreview(usersRoot, "manager-users");
            } catch (Throwable problem) {
                failure.set(problem);
            } finally {
                Platform.exit();
                finished.countDown();
            }
        }));

        if (!finished.await(10, TimeUnit.SECONDS)) throw new AssertionError("JavaFX view load timed out");
        if (failure.get() != null) throw new AssertionError("Authenticated workspace did not load", failure.get());
    }

    private void savePreview(Parent root, String name) throws java.io.IOException {
        var directory = System.getenv("RESOLVEIT_UI_PREVIEW_DIR");
        if (directory == null) return;
        var image = root.snapshot(null, null);
        var buffered = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        }
        javax.imageio.ImageIO.write(buffered, "png", new java.io.File(directory, name + ".png"));
    }

    private SessionState employeeSession() {
        var session = new SessionState();
        session.start(new LoginResponse("token", "Bearer", 900,
                new User(1, "employee01", "employee@example.test", Role.EMPLOYEE, true, null, null)));
        return session;
    }

    private SessionState technicianSession() {
        var session = new SessionState();
        session.start(new LoginResponse("token", "Bearer", 900,
                new User(2, "technician1", "technician@example.test", Role.TECHNICIAN, true, null, null)));
        return session;
    }

    private static final class EmptyTicketClient implements TicketClient {
        @Override public java.util.concurrent.CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size) {
            return CompletableFuture.completedFuture(new PageResponse<>(java.util.List.of(), page, size, 0, 0));
        }
        @Override public java.util.concurrent.CompletionStage<PageResponse<Ticket>> list(
                TicketStatus status, TicketPriority priority, Boolean assignedToMe, Boolean unassigned, int page, int size) {
            return CompletableFuture.completedFuture(new PageResponse<>(java.util.List.of(), page, size, 0, 0));
        }
        @Override public java.util.concurrent.CompletionStage<Ticket> get(int id) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> create(CreateTicket request) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> update(int id, UpdateTicket request) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<PageResponse<TicketMessage>> messages(int id) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<TicketMessage> addComment(int id, CreateMessage request) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> cancel(int id) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> reopen(int id) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> take(int id) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> changeStatus(int id, ChangeStatus request) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> changePriority(int id, ChangePriority request) { return unsupported(); }
        @Override public java.util.concurrent.CompletionStage<Ticket> resolve(int id, ResolveTicket request) { return unsupported(); }

        private static <T> java.util.concurrent.CompletionStage<T> unsupported() {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
