package resolveit.frontend.navigation;

/**
 * Optional lifecycle callbacks a view controller may implement so the navigator
 * can notify it when the view becomes visible and when it is torn down.
 */
public interface ViewLifecycle {
    /** Called after the view is shown; override to start or refresh its state. */
    default void onShown() {
    }

    /** Called when the view is replaced; override to cancel work and release resources. */
    default void dispose() {
    }
}
