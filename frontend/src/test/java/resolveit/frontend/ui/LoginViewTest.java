package resolveit.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.AuthClient;
import resolveit.frontend.auth.AuthService;
import resolveit.frontend.auth.LoginRequest;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.auth.LogoutRequest;
import resolveit.frontend.auth.RefreshRequest;
import resolveit.frontend.auth.RefreshResponse;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.session.SessionState;

class LoginViewTest {
    private static final long FX_TIMEOUT_SECONDS = 10;

    @BeforeAll
    static void startJavaFx() throws Exception {
        var started = new CompletableFuture<Void>();
        Platform.startup(() -> started.complete(null));
        started.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void loadsViewAndCoordinatesValidationPasswordVisibilityAndRequestState() throws Exception {
        var loginResult = new CompletableFuture<LoginResponse>();
        var fixture = onJavaFxThread(() -> loadFixture(loginResult));

        onJavaFxThread(() -> {
            var emailField = fixture.node("#emailField", TextField.class);
            var emailError = fixture.node("#emailErrorLabel", Label.class);
            var passwordLabel = fixture.node("#passwordLabel", Label.class);
            var passwordField = fixture.node("#passwordField", PasswordField.class);
            var visiblePassword = fixture.node("#visiblePasswordField", TextField.class);
            var passwordError = fixture.node("#passwordErrorLabel", Label.class);
            var showPassword = fixture.node("#showPasswordCheckBox", CheckBox.class);
            var loginButton = fixture.node("#loginButton", Button.class);
            var progress = fixture.node("#progressIndicator", ProgressIndicator.class);

            assertSame(passwordField, passwordLabel.getLabelFor());
            assertFalse(emailError.isManaged());
            assertFalse(passwordError.isManaged());
            loginButton.fire();
            assertEquals("Enter your email address.", emailError.getText());
            assertEquals("Enter your password.", passwordError.getText());
            assertTrue(emailError.isManaged());
            assertTrue(passwordError.isManaged());

            emailField.setText("employee@example.test");
            passwordField.setText("secret");
            showPassword.setSelected(true);
            assertSame(visiblePassword, passwordLabel.getLabelFor());
            assertTrue(visiblePassword.isVisible());
            assertTrue(visiblePassword.isManaged());
            assertFalse(passwordField.isVisible());
            assertFalse(passwordField.isManaged());

            loginButton.fire();
            assertEquals(1, fixture.client().loginCalls());
            assertEquals("Signing in…", loginButton.getText());
            assertTrue(loginButton.isDisabled());
            assertTrue(emailField.isDisabled());
            assertTrue(visiblePassword.isDisabled());
            assertTrue(showPassword.isDisabled());
            assertTrue(progress.isVisible());
            assertTrue(progress.isManaged());
            assertSame(loginButton.getParent(), progress.getParent());
            loginButton.fire();
            assertEquals(1, fixture.client().loginCalls());
            return null;
        });

        loginResult.completeExceptionally(new IllegalStateException("Simulated failure"));
        onJavaFxThread(() -> null);

        onJavaFxThread(() -> {
            var emailField = fixture.node("#emailField", TextField.class);
            var visiblePassword = fixture.node("#visiblePasswordField", TextField.class);
            var loginButton = fixture.node("#loginButton", Button.class);
            var progress = fixture.node("#progressIndicator", ProgressIndicator.class);
            var formError = fixture.node("#formErrorLabel", Label.class);

            assertEquals("Sign-in could not be completed. Please try again.", formError.getText());
            assertTrue(formError.isManaged());
            assertEquals("Sign in", loginButton.getText());
            assertFalse(loginButton.isDisabled());
            assertFalse(emailField.isDisabled());
            assertFalse(visiblePassword.isDisabled());
            assertFalse(progress.isVisible());
            assertFalse(progress.isManaged());
            fixture.controller().dispose();
            return null;
        });
    }

    private LoginFixture loadFixture(CompletableFuture<LoginResponse> loginResult) throws Exception {
        var client = new StubAuthClient(loginResult);
        var session = new SessionState();
        var authService = new AuthService(client, session);
        var navigator = new Navigator(null, authService, null, null, null, session);
        var resource = getClass().getResource("/resolveit/frontend/views/login.fxml");
        var loader = new FXMLLoader(resource);
        loader.setControllerFactory(type -> {
            if (type == LoginController.class) {
                return new LoginController(authService, navigator);
            }
            throw new IllegalArgumentException("Unexpected FXML controller: " + type.getName());
        });
        Parent root = loader.load();
        var scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(getClass().getResource(
                "/resolveit/frontend/styles/app.css").toExternalForm());
        root.applyCss();
        root.layout();
        return new LoginFixture(root, loader.getController(), client);
    }

    private static <T> T onJavaFxThread(Callable<T> action) throws Exception {
        var task = new FutureTask<>(action);
        Platform.runLater(task);
        try {
            return task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new AssertionError("JavaFX action failed", exception.getCause());
        }
    }

    private record LoginFixture(Parent root, LoginController controller, StubAuthClient client) {
        private <T> T node(String selector, Class<T> type) {
            return type.cast(root.lookup(selector));
        }
    }

    private static final class StubAuthClient implements AuthClient {
        private final CompletableFuture<LoginResponse> loginResult;
        private int loginCalls;

        private StubAuthClient(CompletableFuture<LoginResponse> loginResult) {
            this.loginResult = loginResult;
        }

        private int loginCalls() {
            return loginCalls;
        }

        @Override
        public CompletionStage<LoginResponse> login(LoginRequest request) {
            loginCalls++;
            return loginResult;
        }

        @Override
        public CompletionStage<RefreshResponse> refresh(RefreshRequest request) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public CompletionStage<Void> logout(LogoutRequest request, String authorization) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
