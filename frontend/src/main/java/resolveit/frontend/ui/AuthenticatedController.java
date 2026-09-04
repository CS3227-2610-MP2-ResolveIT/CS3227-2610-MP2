package resolveit.frontend.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;
import resolveit.frontend.session.SessionState;

public final class AuthenticatedController implements ViewLifecycle {
    private final SessionState session;
    private final Navigator navigator;

    @FXML private Label avatarLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;

    public AuthenticatedController(SessionState session, Navigator navigator) {
        this.session = session;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        var user = session.current().orElseThrow().user();
        welcomeLabel.setText("Welcome, " + user.username());
        emailLabel.setText(user.email());
        roleLabel.setText(user.role().displayName());
        avatarLabel.setText(initials(user.username()));
    }

    @FXML
    private void logout() {
        session.clear();
        navigator.showLogin();
    }

    private String initials(String username) {
        if (username == null || username.isBlank()) {
            return "RI";
        }
        var parts = username.trim().split("[._\\-\\s]+", 2);
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
