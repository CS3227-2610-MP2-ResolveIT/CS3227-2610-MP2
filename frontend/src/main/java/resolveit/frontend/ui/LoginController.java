package resolveit.frontend.ui;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import resolveit.frontend.auth.AuthFailure;
import resolveit.frontend.auth.AuthService;
import resolveit.frontend.auth.LoginValidator;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;

public final class LoginController implements ViewLifecycle {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final AuthService authService;
    private final Navigator navigator;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private CompletableFuture<?> inFlight;
    private boolean disposed;

    @FXML private Label emailLabel;
    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Label passwordErrorLabel;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label formErrorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator progressIndicator;

    public LoginController(AuthService authService, Navigator navigator) {
        this.authService = authService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        emailLabel.setLabelFor(emailField);
        passwordLabel.setLabelFor(passwordField);
        emailField.setAccessibleHelp("Enter the email address assigned to your ResolveIT account.");
        passwordField.setAccessibleHelp("Enter your ResolveIT password.");
        visiblePasswordField.setAccessibleHelp("Enter your ResolveIT password. The password is currently visible.");

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        visiblePasswordField.managedProperty().bind(visiblePasswordField.visibleProperty());
        passwordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());
        passwordField.managedProperty().bind(passwordField.visibleProperty());

        emailField.disableProperty().bind(loading);
        passwordField.disableProperty().bind(loading);
        visiblePasswordField.disableProperty().bind(loading);
        showPasswordCheckBox.disableProperty().bind(loading);
        loginButton.disableProperty().bind(loading);
        progressIndicator.visibleProperty().bind(loading);
        progressIndicator.managedProperty().bind(loading);

        bindManagedToVisible(emailErrorLabel);
        bindManagedToVisible(passwordErrorLabel);
        bindManagedToVisible(formErrorLabel);

        emailField.textProperty().addListener((ignored, oldValue, newValue) -> clearEmailError());
        passwordField.textProperty().addListener((ignored, oldValue, newValue) -> clearPasswordError());
    }

    @Override
    public void onShown() {
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void submitLogin() {
        if (loading.get()) {
            return;
        }
        clearErrors();
        var validation = LoginValidator.validate(emailField.getText(), passwordField.getText());
        if (!validation.isValid()) {
            showFieldError(emailField, emailErrorLabel, validation.emailError());
            showFieldError(passwordField, passwordErrorLabel, validation.passwordError());
            if (validation.passwordError() != null) {
                visiblePasswordField.pseudoClassStateChanged(INVALID, true);
            }
            if (validation.emailError() != null) {
                emailField.requestFocus();
            } else {
                activePasswordField().requestFocus();
            }
            return;
        }

        loading.set(true);
        formErrorLabel.setText("");
        loginButton.setText("Signing in…");
        inFlight = authService.login(emailField.getText(), passwordField.getText()).toCompletableFuture();
        inFlight.whenComplete((response, failure) -> Platform.runLater(() -> completeLogin(failure)));
    }

    private void completeLogin(Throwable failure) {
        if (disposed) {
            return;
        }
        if (failure == null) {
            navigator.showAuthenticated();
            return;
        }
        loading.set(false);
        loginButton.setText("Sign in");
        var cause = unwrap(failure);
        formErrorLabel.setText(cause instanceof AuthFailure authFailure
                ? authFailure.getMessage()
                : "Sign-in could not be completed. Please try again.");
        activePasswordField().requestFocus();
        activePasswordField().selectAll();
    }

    private TextField activePasswordField() {
        return showPasswordCheckBox.isSelected() ? visiblePasswordField : passwordField;
    }

    private Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void showFieldError(TextField field, Label errorLabel, String message) {
        if (message == null) {
            return;
        }
        field.pseudoClassStateChanged(INVALID, true);
        errorLabel.setText(message);
    }

    private void clearEmailError() {
        emailField.pseudoClassStateChanged(INVALID, false);
        emailErrorLabel.setText("");
        formErrorLabel.setText("");
    }

    private void clearPasswordError() {
        passwordField.pseudoClassStateChanged(INVALID, false);
        visiblePasswordField.pseudoClassStateChanged(INVALID, false);
        passwordErrorLabel.setText("");
        formErrorLabel.setText("");
    }

    private void clearErrors() {
        clearEmailError();
        clearPasswordError();
    }

    private void bindManagedToVisible(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    @Override
    public void dispose() {
        disposed = true;
        if (inFlight != null && !inFlight.isDone()) {
            inFlight.cancel(true);
        }
        visiblePasswordField.textProperty().unbindBidirectional(passwordField.textProperty());
    }
}
