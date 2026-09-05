package resolveit.frontend.navigation;

public interface ViewLifecycle {
    default void onShown() {
    }

    default void dispose() {
    }
}
