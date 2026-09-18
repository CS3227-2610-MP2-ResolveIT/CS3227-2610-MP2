package resolveit.frontend.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.application.Platform;

/** Tracks controller-owned asynchronous work and delivers its completion on the JavaFX thread. */
final class AsyncOperationTracker {
    private final Set<CompletableFuture<?>> inFlight = new HashSet<>();

    <T> void run(CompletionStage<T> operation, BooleanSupplier isDisposed,
                 Consumer<T> success, Consumer<Throwable> failure) {
        var future = operation.toCompletableFuture();
        track(future);
        future.whenComplete((result, problem) -> Platform.runLater(() -> {
            complete(future);
            if (isDisposed.getAsBoolean()) {
                return;
            }
            if (problem == null) {
                success.accept(result);
            } else {
                failure.accept(problem);
            }
        }));
    }

    void track(CompletableFuture<?> future) {
        inFlight.add(future);
    }

    void complete(CompletableFuture<?> future) {
        inFlight.remove(future);
    }

    void cancelAll() {
        for (var future : Set.copyOf(inFlight)) {
            future.cancel(true);
        }
        inFlight.clear();
    }
}
