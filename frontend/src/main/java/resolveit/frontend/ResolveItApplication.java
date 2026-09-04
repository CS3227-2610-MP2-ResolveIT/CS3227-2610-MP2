package resolveit.frontend;

import javafx.application.Application;
import javafx.stage.Stage;
import resolveit.frontend.auth.AuthService;
import resolveit.frontend.auth.HttpAuthClient;
import resolveit.frontend.config.AppConfig;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.session.SessionState;

public final class ResolveItApplication extends Application {
    private HttpAuthClient authClient;

    @Override
    public void start(Stage stage) {
        var config = AppConfig.fromEnvironment(System.getenv());
        authClient = HttpAuthClient.create(config.apiBaseUrl());
        var session = new SessionState();
        var authService = new AuthService(authClient, session);
        var navigator = new Navigator(stage, authService, session);

        stage.setTitle("ResolveIT");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        navigator.showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (authClient != null) {
            authClient.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
