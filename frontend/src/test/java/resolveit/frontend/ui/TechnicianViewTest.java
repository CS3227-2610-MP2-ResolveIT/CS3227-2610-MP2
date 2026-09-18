package resolveit.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static resolveit.frontend.ui.JavaFxTestSupport.onJavaFxThread;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TechnicianTicketService.MessageType;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketCategory;
import resolveit.frontend.ticket.TicketClient;
import resolveit.frontend.ticket.TicketMessage;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;
import resolveit.frontend.ticket.TicketStatus;

class TechnicianViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        JavaFxTestSupport.startJavaFx();
    }

    @Test
    void queueRefreshFailureDisablesStaleTicketsUntilRecovery() throws Exception {
        var client = new StubTicketClient();
        var initialList = client.queueList();
        var fixture = onJavaFxThread(() -> loadFixture(client));

        try {
            onJavaFxThread(() -> {
                var queueProgress = fixture.node("#queueProgress", ProgressIndicator.class);
                var queueBusy = fixture.node("#queueBusyLabel", Label.class);
                var table = fixture.ticketsTable();
                assertTrue(queueProgress.isVisible());
                assertEquals("Loading tickets…", queueBusy.getText());
                assertTrue(table.isDisabled());
                return null;
            });

            initialList.complete(page(List.of(ticket())));
            onJavaFxThread(() -> null);

            var failedList = client.queueList();
            onJavaFxThread(() -> {
                fixture.node("#refreshQueueButton", Button.class).fire();
                assertTrue(fixture.ticketsTable().isDisabled());
                assertEquals("Refreshing queue…", fixture.node("#queueBusyLabel", Label.class).getText());
                return null;
            });
            failedList.completeExceptionally(new IllegalStateException("offline"));
            onJavaFxThread(() -> null);

            onJavaFxThread(() -> {
                assertTrue(fixture.node("#queueErrorLabel", Label.class).getText()
                        .contains("Previously loaded tickets may be out of date"));
                assertTrue(fixture.ticketsTable().isDisabled());
                assertFalse(fixture.node("#refreshQueueButton", Button.class).isDisabled());
                return null;
            });

            var recoveredList = client.queueList();
            onJavaFxThread(() -> {
                fixture.node("#refreshQueueButton", Button.class).fire();
                return null;
            });
            recoveredList.complete(page(List.of(ticket())));
            onJavaFxThread(() -> {
                assertFalse(fixture.ticketsTable().isDisabled());
                return null;
            });
        } finally {
            dispose(fixture);
        }
    }

    @Test
    void ticketDetailUsesKeyboardNavigationAndClearAudienceControls() throws Exception {
        var client = new StubTicketClient();
        var fixture = loadFixtureWithQueue(client);
        var detail = client.queueDetail();
        var messages = client.queueMessages();

        try {
            onJavaFxThread(() -> {
                var root = fixture.root();
                var table = fixture.ticketsTable();
                assertFilterLabel(fixture, "#statusFilter", "Status");
                assertFilterLabel(fixture, "#priorityFilter", "Priority");
                assertFilterLabel(fixture, "#assignmentFilter", "Assignment");
                assertTrue(fixture.node("#queueToolbar", FlowPane.class).isManaged());
                assertTrue(fixture.node("#queueFooter", FlowPane.class).isManaged());
                assertTrue(fixture.node("#detailMetadata", FlowPane.class).isManaged());
                assertTrue(table.getColumns().getFirst().getCellFactory() != null);
                table.getSelectionModel().selectFirst();
                table.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false));
                assertFalse(fixture.node("#queuePage", StackPane.class).isVisible());
                assertTrue(fixture.node("#detailPage", StackPane.class).isVisible());
                assertEquals(1, fixture.node("#queueNavButton", Button.class).getStyleClass().stream()
                        .filter("nav-button-active"::equals).count());
                assertTrue(root.lookup("#detailBusyLabel").isVisible());
                return null;
            });

            detail.complete(ticket());
            messages.complete(page(List.of(internalNote())));
            onJavaFxThread(() -> {
                var messageType = fixture.messageTypeField();
                var messageField = fixture.node("#messageField", TextArea.class);
                var messageButton = fixture.node("#addMessageButton", Button.class);
                var detailContent = fixture.node("#detailContent", VBox.class);
                assertTrue(detailContent.isVisible());
                assertFalse(detailContent.isDisabled());
                var resolveBox = fixture.node("#resolveBox", VBox.class);
                var priorityRow = fixture.node("#priorityField", ComboBox.class).getParent();
                var actionCard = (VBox) resolveBox.getParent();
                assertTrue(actionCard.getChildren().indexOf(resolveBox)
                        < actionCard.getChildren().indexOf(priorityRow));
                assertTrue(fixture.node("#reopenButton", Button.class).getStyleClass()
                        .contains("primary-button"));
                assertTrue(fixture.node("#cancelButton", Button.class).getStyleClass()
                        .contains("danger-button"));
                assertEquals("Write an update the requester can see…", messageField.getPromptText());
                assertEquals("_Post public comment", messageButton.getText());
                messageType.getSelectionModel().select(MessageType.INTERNAL_NOTE);
                assertEquals("Write a note visible only to IT staff…", messageField.getPromptText());
                assertEquals("_Add internal note", messageButton.getText());
                return null;
            });
        } finally {
            dispose(fixture);
        }
    }

    @Test
    void failedDetailRefreshDisablesStaleTicketActions() throws Exception {
        var client = new StubTicketClient();
        var fixture = loadFixtureWithQueue(client);
        var detail = client.queueDetail();
        var messages = client.queueMessages();

        try {
            openFirstTicket(fixture);
            detail.complete(ticket());
            messages.complete(page(List.of()));
            onJavaFxThread(() -> null);

            var failedDetail = client.queueDetail();
            var failedMessages = client.queueMessages();
            onJavaFxThread(() -> {
                fixture.node("#refreshDetailButton", Button.class).fire();
                assertTrue(fixture.node("#detailContent", VBox.class).isDisabled());
                assertEquals("Refreshing ticket…", fixture.node("#detailBusyLabel", Label.class).getText());
                return null;
            });
            failedDetail.completeExceptionally(new IllegalStateException("offline"));
            failedMessages.complete(page(List.of()));
            onJavaFxThread(() -> {
                var error = fixture.node("#detailErrorLabel", Label.class);
                var detailContent = fixture.node("#detailContent", VBox.class);
                assertTrue(error.getText().contains("Displayed details may be out of date"));
                assertTrue(detailContent.isDisabled());
                assertFalse(fixture.node("#refreshDetailButton", Button.class).isDisabled());
                return null;
            });
        } finally {
            dispose(fixture);
        }
    }

    private TechnicianFixture loadFixtureWithQueue(StubTicketClient client) throws Exception {
        var initialList = client.queueList();
        var fixture = onJavaFxThread(() -> loadFixture(client));
        initialList.complete(page(List.of(ticket())));
        onJavaFxThread(() -> null);
        return fixture;
    }

    private void openFirstTicket(TechnicianFixture fixture) throws Exception {
        onJavaFxThread(() -> {
            var table = fixture.ticketsTable();
            table.getSelectionModel().selectFirst();
            table.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                    false, false, false, false));
            return null;
        });
    }

    private void dispose(TechnicianFixture fixture) throws Exception {
        onJavaFxThread(() -> {
            fixture.controller().dispose();
            return null;
        });
    }

    private TechnicianFixture loadFixture(StubTicketClient client) throws Exception {
        var session = new SessionState();
        var user = new User(7, "technician1", "technician1@resolveit.local",
                Role.TECHNICIAN, true, "2026-09-12T00:00:00Z", "2026-09-12T00:00:00Z");
        session.start(new LoginResponse("access", "refresh", "Bearer", 3600, 7200, user));
        var controller = new TechnicianController(
                session, new TechnicianTicketService(client), null, null);
        var resource = getClass().getResource("/resolveit/frontend/views/technician.fxml");
        var loader = new FXMLLoader(resource);
        loader.setControllerFactory(type -> {
            if (type == TechnicianController.class) {
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
        return new TechnicianFixture(root, controller);
    }

    private static Ticket ticket() {
        return new Ticket(3, "TKT-6117FFFD773F410C", "Cannot connect to VPN",
                "The VPN client reports that the gateway is unavailable.", TicketCategory.NETWORK,
                TicketPriority.HIGH, TicketStatus.IN_PROGRESS, 2, "employee01", 7,
                "technician1", null, "2026-09-12T03:20:00Z", "2026-09-12T03:25:00Z", null, 2);
    }

    private static TicketMessage internalNote() {
        return new TicketMessage(4, 3, 7, "technician1", "INTERNAL_NOTE",
                "Check the gateway before contacting the requester.", "2026-09-12T03:26:00Z");
    }

    private static <T> PageResponse<T> page(List<T> content) {
        return new PageResponse<>(content, 0, 20, content.size(), content.isEmpty() ? 0 : 1);
    }

    private static void assertFilterLabel(TechnicianFixture fixture, String selector, String text) {
        ComboBox<?> filter = fixture.node(selector, ComboBox.class);
        var label = (Label) filter.getParent().lookup(".field-label");
        assertEquals(text, label.getText());
        assertSame(filter, label.getLabelFor());
    }

    private record TechnicianFixture(Parent root, TechnicianController controller) {
        private <T> T node(String selector, Class<T> type) {
            return type.cast(root.lookup(selector));
        }

        @SuppressWarnings("unchecked")
        private TableView<Ticket> ticketsTable() {
            return (TableView<Ticket>) root.lookup("#ticketsTable");
        }

        @SuppressWarnings("unchecked")
        private ComboBox<MessageType> messageTypeField() {
            return (ComboBox<MessageType>) root.lookup("#messageTypeField");
        }
    }

    private static final class StubTicketClient implements TicketClient {
        private final ArrayDeque<CompletableFuture<PageResponse<Ticket>>> lists = new ArrayDeque<>();
        private final ArrayDeque<CompletableFuture<Ticket>> details = new ArrayDeque<>();
        private final ArrayDeque<CompletableFuture<PageResponse<TicketMessage>>> messages = new ArrayDeque<>();

        private CompletableFuture<PageResponse<Ticket>> queueList() {
            var future = new CompletableFuture<PageResponse<Ticket>>();
            lists.add(future);
            return future;
        }

        private CompletableFuture<Ticket> queueDetail() {
            var future = new CompletableFuture<Ticket>();
            details.add(future);
            return future;
        }

        private CompletableFuture<PageResponse<TicketMessage>> queueMessages() {
            var future = new CompletableFuture<PageResponse<TicketMessage>>();
            messages.add(future);
            return future;
        }

        @Override
        public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size) {
            return unsupported();
        }

        @Override
        public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                           Boolean assignedToMe, Boolean unassigned,
                                                           int page, int size) {
            return lists.remove();
        }

        @Override public CompletionStage<Ticket> get(int ticketId) { return details.remove(); }
        @Override public CompletionStage<PageResponse<TicketMessage>> messages(int ticketId) {
            return messages.remove();
        }
        @Override public CompletionStage<Ticket> create(CreateTicket request) { return unsupported(); }
        @Override public CompletionStage<Ticket> update(int ticketId, UpdateTicket request) {
            return unsupported();
        }
        @Override public CompletionStage<TicketMessage> addComment(int ticketId, CreateMessage request) {
            return unsupported();
        }
        @Override public CompletionStage<Ticket> cancel(int ticketId) { return unsupported(); }
        @Override public CompletionStage<Ticket> reopen(int ticketId) { return unsupported(); }
        @Override public CompletionStage<Ticket> take(int ticketId) { return unsupported(); }
        @Override public CompletionStage<Ticket> changeStatus(int ticketId, ChangeStatus request) {
            return unsupported();
        }
        @Override public CompletionStage<Ticket> changePriority(int ticketId, ChangePriority request) {
            return unsupported();
        }
        @Override public CompletionStage<Ticket> resolve(int ticketId, ResolveTicket request) {
            return unsupported();
        }

        private static <T> CompletionStage<T> unsupported() {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
