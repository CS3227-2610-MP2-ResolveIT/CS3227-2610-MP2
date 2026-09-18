package resolveit.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.EmployeeTicketService;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;
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

class AuthenticatedViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        JavaFxTestSupport.startJavaFx();
    }

    @Test
    void ticketRefreshFailureShowsFeedbackAndRestoresControls() throws Exception {
        var client = new StubTicketClient();
        var initialList = client.nextList();
        var fixture = onJavaFxThread(() -> loadFixture(client));

        try {
            initialList.complete(page(List.of()));
            onJavaFxThread(() -> null);
            var failedList = client.nextList();
            onJavaFxThread(() -> {
                fixture.node("#refreshTicketsButton", Button.class).fire();
                assertTrue(fixture.node("#ticketsProgress", ProgressIndicator.class).isVisible());
                assertTrue(fixture.node("#refreshTicketsButton", Button.class).isDisabled());
                return null;
            });
            failedList.completeExceptionally(new IllegalStateException("offline"));
            onJavaFxThread(() -> {
                var error = fixture.node("#ticketsErrorLabel", Label.class);
                assertTrue(error.getText().contains("could not be completed"));
                assertFalse(fixture.node("#ticketsProgress", ProgressIndicator.class).isVisible());
                assertFalse(fixture.node("#refreshTicketsButton", Button.class).isDisabled());
                return null;
            });
        } finally {
            dispose(fixture);
        }
    }

    @Test
    void disposalCancelsOutstandingTicketRefresh() throws Exception {
        var client = new StubTicketClient();
        var pendingList = client.nextList();
        var fixture = onJavaFxThread(() -> loadFixture(client));

        onJavaFxThread(() -> {
            fixture.controller().dispose();
            return null;
        });

        assertTrue(pendingList.isCancelled());
    }

    private AuthenticatedFixture loadFixture(StubTicketClient client) throws Exception {
        var session = new SessionState();
        var user = new User(3, "employee1", "employee1@resolveit.local", Role.EMPLOYEE,
                true, "2026-09-12T00:00:00Z", "2026-09-12T00:00:00Z");
        session.start(new LoginResponse("access", "refresh", "Bearer", 3600, 7200, user));
        var controller = new AuthenticatedController(session, new EmployeeTicketService(client), null);
        var loader = new FXMLLoader(getClass().getResource("/resolveit/frontend/views/authenticated.fxml"));
        loader.setControllerFactory(type -> {
            if (type == AuthenticatedController.class) {
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
        return new AuthenticatedFixture(root, controller);
    }

    private void dispose(AuthenticatedFixture fixture) throws Exception {
        onJavaFxThread(() -> {
            fixture.controller().dispose();
            return null;
        });
    }

    private static <T> PageResponse<T> page(List<T> content) {
        return new PageResponse<>(content, 0, 20, content.size(), content.isEmpty() ? 0 : 1);
    }

    private record AuthenticatedFixture(Parent root, AuthenticatedController controller) {
        private <T> T node(String selector, Class<T> type) {
            return type.cast(root.lookup(selector));
        }
    }

    private static final class StubTicketClient implements TicketClient {
        private final ArrayDeque<CompletableFuture<PageResponse<Ticket>>> lists = new ArrayDeque<>();

        private CompletableFuture<PageResponse<Ticket>> nextList() {
            var future = new CompletableFuture<PageResponse<Ticket>>();
            lists.add(future);
            return future;
        }

        @Override
        public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size) {
            return lists.remove();
        }

        @Override
        public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                           Boolean assignedToMe, Boolean unassigned,
                                                           int page, int size) {
            return unsupported();
        }

        @Override public CompletionStage<Ticket> get(int ticketId) { return unsupported(); }
        @Override public CompletionStage<PageResponse<TicketMessage>> messages(int ticketId) {
            return unsupported();
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
