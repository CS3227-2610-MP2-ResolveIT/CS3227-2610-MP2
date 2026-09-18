package resolveit.frontend.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.application.Platform;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketMessage;

/** Loads the ticket and message history needed to display one Technician ticket detail view. */
final class TechnicianTicketDetailLoader {
    private final TechnicianTicketService ticketService;
    private final AsyncOperationTracker operations;

    TechnicianTicketDetailLoader(TechnicianTicketService ticketService, AsyncOperationTracker operations) {
        this.ticketService = ticketService;
        this.operations = operations;
    }

    void load(int ticketId, BooleanSupplier isDisposed, Consumer<DetailData> success,
              Consumer<Throwable> failure) {
        var ticketFuture = ticketService.get(ticketId).toCompletableFuture();
        var messagesFuture = ticketService.messages(ticketId).toCompletableFuture();
        var combined = ticketFuture.thenCombine(messagesFuture, DetailData::new);
        operations.track(ticketFuture);
        operations.track(messagesFuture);
        operations.track(combined);
        combined.whenComplete((data, problem) -> Platform.runLater(() -> {
            operations.complete(ticketFuture);
            operations.complete(messagesFuture);
            operations.complete(combined);
            if (isDisposed.getAsBoolean()) {
                return;
            }
            if (problem == null) {
                success.accept(data);
            } else {
                failure.accept(problem);
            }
        }));
    }

    record DetailData(Ticket ticket, PageResponse<TicketMessage> messages) {}
}
