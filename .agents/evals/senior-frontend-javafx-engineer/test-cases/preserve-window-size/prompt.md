Investigate and fix the JavaFX window resizing issue in this repository.

When the application window is maximized or manually resized, navigating between
top-level views such as login, the authenticated workspace, and User Management
causes the window to return to a fixed size.

Find the root cause and implement the smallest safe fix. Preserve the existing
navigation lifecycle, controller disposal, stylesheet behavior, and `onShown()`
ordering. Do not perform a broad UI redesign.

Run the relevant available checks and report what was actually verified.