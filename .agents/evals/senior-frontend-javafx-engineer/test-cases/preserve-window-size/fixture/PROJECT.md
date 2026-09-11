# ResolveIT navigation slice

This is the shared top-level navigation boundary of the JavaFX client, reduced
to its relevant production classes. Login, workspace, and User Management each
provide a `View`; their controllers and backend services are outside this slice.
The application creates one `Navigator` for its primary Stage and calls `show`
on the JavaFX Application Thread. No FXML loader or application launcher is
included. Use Java 25 and JavaFX 25; do not add frameworks.

Navigation disposes the outgoing controller before installing the next view.
`onShown()` runs after the new root is attached and the Stage is shown. Keep the
1120 by 720 initial content size and the shared stylesheet. Subsequent navigation
must respect the user's manually resized or maximized window.

Run `python3 run-checks.py` to compile this slice. Set `JAVAFX_LIB` to an existing
JavaFX 25 SDK `lib` directory. No GUI tests or dependency downloader are configured.
If the SDK is unavailable, report that limitation. In the full client, manually
check login → workspace → User Management → logout at default, manually resized,
and maximized sizes, including repeated navigation and styling.
