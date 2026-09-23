package resolveit.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static resolveit.frontend.ui.JavaFxTestSupport.onJavaFxThread;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.user.ManagerClient;
import resolveit.frontend.user.ManagerService;

class UsersViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        JavaFxTestSupport.startJavaFx();
        // Closing the confirmation must not shut down the shared test toolkit.
        onJavaFxThread(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @Test
    void cancellingAccessChangeDoesNotUpdateUser() throws Exception {
        var client = new StubManagerClient();
        var initialList = client.nextList();
        var fixture = onJavaFxThread(() -> loadFixture(client));
        var technician = new User(9, "support", "support@example.test", Role.TECHNICIAN,
                true, "2026-09-12T00:00:00Z", "2026-09-12T00:00:00Z");
        var confirmationCancelled = new CompletableFuture<Void>();

        try {
            initialList.complete(new PageResponse<>(List.of(technician), 0, 20, 1, 1));
            onJavaFxThread(() -> {
                fixture.usersTable().getSelectionModel().selectFirst();
                assertEquals(Role.TECHNICIAN, fixture.roleField().getValue());
                assertTrue(fixture.node("#activeField", CheckBox.class).isSelected());
                fixture.roleField().setValue(Role.EMPLOYEE);

                // showAndWait runs a nested event loop, which processes this cancellation.
                Platform.runLater(() -> {
                    try {
                        var dialog = Window.getWindows().stream()
                                .map(window -> window.getScene().lookup(".dialog-pane"))
                                .filter(DialogPane.class::isInstance)
                                .map(DialogPane.class::cast)
                                .findFirst().orElseThrow();
                        // Cancel detaches the pane from its scene, so retain the window for cleanup.
                        var dialogWindow = dialog.getScene().getWindow();
                        try {
                            assertEquals("Change account access", dialog.getHeaderText());
                            assertEquals("Save access changes for support?", dialog.getContentText());
                            assertEquals(0, client.updateCalls);
                            assertEquals(0, client.createCalls);
                            ((Button) dialog.lookupButton(ButtonType.CANCEL)).fire();
                        } finally {
                            dialogWindow.hide();
                        }
                        confirmationCancelled.complete(null);
                    } catch (Throwable failure) {
                        confirmationCancelled.completeExceptionally(failure);
                    }
                });
                fixture.node("#saveButton", Button.class).fire();
                return null;
            });
            confirmationCancelled.get(10, TimeUnit.SECONDS);

            onJavaFxThread(() -> {
                assertEquals(0, client.updateCalls);
                assertEquals(0, client.createCalls);
                assertEquals(List.of(technician), fixture.usersTable().getItems());
                assertEquals(Role.TECHNICIAN, fixture.usersTable().getItems().getFirst().role());
                assertTrue(fixture.usersTable().getItems().getFirst().active());
                assertFalse(fixture.node("#saveButton", Button.class).isDisabled());
                return null;
            });
        } finally {
            onJavaFxThread(() -> {
                fixture.controller().dispose();
                return null;
            });
        }
    }

    private UsersFixture loadFixture(StubManagerClient client) throws Exception {
        var session = new SessionState();
        var manager = new User(7, "manager", "manager@example.test", Role.MANAGER,
                true, "2026-09-12T00:00:00Z", "2026-09-12T00:00:00Z");
        session.start(new LoginResponse("access", "refresh", "Bearer", 3600, 7200, manager));
        var controller = new UsersController(session, new ManagerService(client), null);
        var loader = new FXMLLoader(getClass().getResource("/resolveit/frontend/views/users.fxml"));
        loader.setControllerFactory(type -> {
            if (type == UsersController.class) {
                return controller;
            }
            throw new IllegalArgumentException("Unexpected FXML controller: " + type.getName());
        });
        Parent root = loader.load();
        var scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(getClass().getResource(
                "/resolveit/frontend/styles/app.css").toExternalForm());
        root.applyCss();
        root.layout();
        controller.onShown();
        return new UsersFixture(root, controller);
    }

    private record UsersFixture(Parent root, UsersController controller) {
        private <T> T node(String selector, Class<T> type) {
            return type.cast(root.lookup(selector));
        }

        @SuppressWarnings("unchecked")
        private TableView<User> usersTable() {
            return (TableView<User>) root.lookup("#usersTable");
        }

        @SuppressWarnings("unchecked")
        private ComboBox<Role> roleField() {
            return (ComboBox<Role>) root.lookup("#roleField");
        }
    }

    private static final class StubManagerClient implements ManagerClient {
        private final ArrayDeque<CompletableFuture<PageResponse<User>>> lists = new ArrayDeque<>();
        private int createCalls;
        private int updateCalls;

        private CompletableFuture<PageResponse<User>> nextList() {
            var future = new CompletableFuture<PageResponse<User>>();
            lists.add(future);
            return future;
        }

        @Override public CompletionStage<PageResponse<User>> users(int page) { return lists.remove(); }
        @Override public CompletionStage<List<User>> technicians() { return unsupported(); }
        @Override public CompletionStage<Ticket> assign(int id, int technicianId) { return unsupported(); }

        @Override public CompletionStage<User> createUser(UserRequest request) {
            createCalls++;
            return unsupported();
        }

        @Override public CompletionStage<User> updateUser(int id, UserRequest request) {
            updateCalls++;
            return unsupported();
        }

        private static <T> CompletionStage<T> unsupported() {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
