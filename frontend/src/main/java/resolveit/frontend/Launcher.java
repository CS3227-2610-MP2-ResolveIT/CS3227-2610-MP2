package resolveit.frontend;

/** Starts JavaFX without triggering the JDK's module-path-only JavaFX launcher. */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        ResolveItApplication.main(args);
    }
}
