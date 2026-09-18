package resolveit.frontend.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

final class JavaFxTestSupport {
    private static final long FX_TIMEOUT_SECONDS = 10;
    private static boolean started;

    private JavaFxTestSupport() {
    }

    static synchronized void startJavaFx() throws Exception {
        if (started) {
            return;
        }
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> ready.complete(null));
        ready.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        started = true;
    }

    static <T> T onJavaFxThread(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError("JavaFX action failed", exception.getCause());
        }
    }
}
