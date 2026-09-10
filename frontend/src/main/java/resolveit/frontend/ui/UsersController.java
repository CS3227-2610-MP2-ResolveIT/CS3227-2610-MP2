package resolveit.frontend.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.user.ManagerClient.UserRequest;
import resolveit.frontend.user.ManagerService;

public final class UsersController implements ViewLifecycle {
    private final SessionState session;
    private final ManagerService service;
    private final Navigator navigator;
    private final Set<CompletableFuture<?>> pending = new HashSet<>();
    private boolean disposed, busy;
    private int page, pages;
    private User editing;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn, emailColumn, roleColumn, activeColumn;
    @FXML private TextField usernameField, emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleField;
    @FXML private CheckBox activeField;
    @FXML private Label errorLabel, noticeLabel, pageLabel, editorTitle, profileLabel;
    @FXML private ProgressIndicator progress;
    @FXML private Button previousButton, nextButton, refreshButton, newButton, saveButton, backButton;
    @FXML private VBox editor;

    public UsersController(SessionState session, ManagerService service, Navigator navigator) {
        this.session = session;
        this.service = service;
        this.navigator = navigator;
    }
    @FXML private void initialize() {
        profileLabel.setText(session.current().orElseThrow().user().username() + " · Manager");
        usernameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().username()));
        emailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().email()));
        roleColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().role().name()));
        activeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().active() ? "Active" : "Inactive"));
        roleField.setItems(FXCollections.observableArrayList(Role.values()));
        usersTable.getSelectionModel().selectedItemProperty().addListener((o, old, user) -> {
            if (!busy && user != null) edit(user);
        });
        for (var label : new Label[] {errorLabel, noticeLabel}) {
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            label.managedProperty().bind(label.visibleProperty());
        }
        newUser();
    }
    @Override public void onShown() { refresh(); }
    @FXML private void back() { navigator.showAuthenticated(); }
    @FXML private void logout() { session.clear(); navigator.showLogin(); }
    @FXML private void newUser() {
        if (busy) return;
        editing = null;
        usersTable.getSelectionModel().clearSelection();
        editorTitle.setText("Create user");
        usernameField.clear(); emailField.clear(); passwordField.clear();
        roleField.setValue(Role.EMPLOYEE); activeField.setSelected(true);
        errorLabel.setText("");
        usernameField.requestFocus();
    }
    private void edit(User user) {
        editing = user;
        editorTitle.setText("Edit " + user.username());
        usernameField.setText(user.username()); emailField.setText(user.email()); passwordField.clear();
        roleField.setValue(user.role()); activeField.setSelected(user.active());
        errorLabel.setText(""); noticeLabel.setText("");
    }
    @FXML private void refresh() {
        if (busy || disposed) return;
        setBusy(true);
        errorLabel.setText("");
        run(service.users(page), result -> {
            usersTable.setItems(FXCollections.observableArrayList(result.content()));
            page = result.page(); pages = result.totalPages();
            pageLabel.setText((pages == 0 ? "Page 0 of 0" : "Page " + (page + 1) + " of " + pages) + " · " + result.totalElements() + " users");
            setBusy(false);
        });
    }
    @FXML private void previous() { if (!busy && page > 0) { page--; refresh(); } }
    @FXML private void next() { if (!busy && page + 1 < pages) { page++; refresh(); } }
    @FXML private void save() {
        if (busy) return;
        var error = ManagerService.validate(usernameField.getText(), emailField.getText(), passwordField.getText(), editing == null);
        if (error != null || roleField.getValue() == null) {
            errorLabel.setText(error == null ? "Choose a role." : error);
            return;
        }
        if (editing != null && (editing.active() != activeField.isSelected() || editing.role() != roleField.getValue())) {
            var alert = new Alert(Alert.AlertType.CONFIRMATION, "Save access changes for " + editing.username() + "?"
                    + (editing.id() == session.current().orElseThrow().user().id() ? " You will be signed out after changing your own access." : ""), ButtonType.CANCEL, ButtonType.OK);
            alert.setHeaderText("Change account access");
            if (alert.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
        }
        Integer id = editing == null ? null : editing.id();
        var password = passwordField.getText();
        var request = new UserRequest(usernameField.getText().trim(), emailField.getText().trim(),
                id != null && password.isEmpty() ? null : password, roleField.getValue(), activeField.isSelected());
        setBusy(true); errorLabel.setText(""); noticeLabel.setText("");
        run(service.save(id, request), saved -> {
            passwordField.clear();
            if (saved.id() == session.current().orElseThrow().user().id()) {
                session.clear(); navigator.showLogin(); return;
            }
            setBusy(false); edit(saved); noticeLabel.setText("User saved."); refresh();
        });
    }
    private void setBusy(boolean value) {
        busy = value;
        progress.setVisible(value); progress.setManaged(value);
        editor.setDisable(value); usersTable.setDisable(value);
        refreshButton.setDisable(value); newButton.setDisable(value); saveButton.setDisable(value); backButton.setDisable(value);
        previousButton.setDisable(value || page == 0); nextButton.setDisable(value || page + 1 >= pages);
    }
    private <T> void run(CompletionStage<T> operation, Consumer<T> success) {
        var future = operation.toCompletableFuture(); pending.add(future);
        future.whenComplete((result, failure) -> Platform.runLater(() -> {
            pending.remove(future);
            if (disposed) return;
            if (failure == null) { success.accept(result); return; }
            setBusy(false);
            var cause = failure;
            while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof TicketFailure problem) {
                if (problem.kind() == TicketFailure.Kind.UNAUTHORIZED) { session.clear(); navigator.showLogin(); return; }
                errorLabel.setText(problem.getMessage());
            } else errorLabel.setText("Unable to complete the request. Please try again.");
        }));
    }
    @Override public void dispose() {
        disposed = true; passwordField.clear();
        for (var future : Set.copyOf(pending)) future.cancel(true);
        pending.clear();
    }
}
