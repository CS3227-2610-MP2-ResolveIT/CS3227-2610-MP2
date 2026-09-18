package resolveit.frontend.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TechnicianTicketService.MessageType;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.ticket.TicketMessage;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketStatus;
import resolveit.frontend.ticket.TicketValidator;
import resolveit.frontend.user.ManagerService;

/** Owns loading, rendering, validation, and support actions for one technician ticket. */
public final class TechnicianTicketDetailController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");
    private final SessionState session;
    private final TechnicianTicketService ticketService;
    private final ManagerService managerService;
    private final Navigator navigator;
    private final AsyncOperationTracker operations = new AsyncOperationTracker();
    private final TechnicianTicketDetailRenderer renderer = new TechnicianTicketDetailRenderer();
    private final TechnicianTicketDetailLoader loader;
    private Runnable queueHandler = () -> { };
    private Runnable queueRefreshHandler = () -> { };
    private Consumer<Boolean> busyHandler = ignored -> { };
    private Ticket selectedTicket;
    private Integer ticketId;
    private boolean disposed;
    private boolean loading;
    private boolean stale;
    private boolean actionLoading;
    private boolean assigneesLoading;

    @FXML private Label detailNumberLabel, detailStatusLabel, detailErrorLabel, detailNoticeLabel;
    @FXML private Label detailBusyLabel, detailSubjectLabel, detailDescriptionLabel, detailCategoryLabel;
    @FXML private Label detailPriorityLabel, detailRequesterLabel, detailAssigneeLabel, detailCreatedLabel;
    @FXML private Label resolutionLabel, resolutionErrorLabel, messageErrorLabel, assigneeFieldLabel;
    @FXML private Label priorityFieldLabel, resolutionFieldLabel, messageTypeFieldLabel, messageFieldLabel;
    @FXML private HBox detailNoticeBox;
    @FXML private StackPane detailNoticeMark;
    @FXML private ProgressIndicator detailProgress;
    @FXML private VBox detailContent, resolutionBox, resolveBox, assignmentBox;
    @FXML private Button refreshDetailButton, takeButton, beginWorkButton, cancelButton, reopenButton;
    @FXML private Button changePriorityButton, resolveButton, addMessageButton, assignButton, refreshAssigneesButton;
    @FXML private ComboBox<TicketPriority> priorityField;
    @FXML private ComboBox<MessageType> messageTypeField;
    @FXML private ComboBox<User> assigneeField;
    @FXML private TextArea resolutionField, messageField;
    @FXML private ListView<TicketMessage> messagesList;

    /**
     * Creates the ticket-detail child controller for technician and manager actions.
     *
     * @param session current session used for role and ownership decisions
     * @param ticketService service used for technician ticket and message operations
     * @param managerService service used for manager-only assignment operations
     * @param navigator navigator used to return to login after authorization failure
     */
    public TechnicianTicketDetailController(SessionState session, TechnicianTicketService ticketService,
                                            ManagerService managerService, Navigator navigator) {
        this.session = session;
        this.ticketService = ticketService;
        this.managerService = managerService;
        this.navigator = navigator;
        loader = new TechnicianTicketDetailLoader(ticketService, operations);
    }

    @FXML
    private void initialize() {
        assigneeFieldLabel.setLabelFor(assigneeField);
        priorityFieldLabel.setLabelFor(priorityField);
        resolutionFieldLabel.setLabelFor(resolutionField);
        messageTypeFieldLabel.setLabelFor(messageTypeField);
        messageFieldLabel.setLabelFor(messageField);
        var manager = isManager();
        assignmentBox.setVisible(manager);
        assignmentBox.setManaged(manager);
        priorityField.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        priorityField.setConverter(Converters.nullable("", TicketPriority::displayName));
        messageTypeField.setItems(FXCollections.observableArrayList(MessageType.values()));
        messageTypeField.setConverter(Converters.nullable("", MessageType::displayName));
        messageTypeField.setValue(MessageType.PUBLIC_COMMENT);
        assigneeField.setConverter(Converters.nullable("Choose an active technician or manager",
                user -> user.username() + " (" + user.role() + ")"));
        messageTypeField.valueProperty().addListener((ignored, oldValue, newValue) -> updateMessageCopy(newValue));
        priorityField.valueProperty().addListener((ignored, oldValue, newValue) -> updateBusyState());
        assigneeField.valueProperty().addListener((ignored, oldValue, newValue) -> updateBusyState());
        resolutionField.textProperty().addListener((ignored, oldValue, newValue) -> resolutionErrorLabel.setText(""));
        messageField.textProperty().addListener((ignored, oldValue, newValue) -> messageErrorLabel.setText(""));
        messagesList.setCellFactory(ignored -> new MessageCell());
        bindManaged(detailErrorLabel);
        bindManaged(detailNoticeBox, detailNoticeLabel);
        bindManaged(resolutionErrorLabel);
        bindManaged(messageErrorLabel);
        updateMessageCopy(messageTypeField.getValue());
    }

    void setQueueHandler(Runnable handler) { queueHandler = handler; }
    void setQueueRefreshHandler(Runnable handler) { queueRefreshHandler = handler; }
    void setBusyHandler(Consumer<Boolean> handler) { busyHandler = handler; }
    void dispose() { disposed = true; operations.cancelAll(); }

    void openTicket(int id) {
        if (loading || actionLoading) { return; }
        ticketId = id;
        stale = false;
        assigneeField.setValue(null);
        selectedTicket = null;
        detailNumberLabel.setText("Ticket details");
        detailContent.setVisible(false);
        detailContent.setManaged(false);
        loadDetails(id, null);
        if (isManager()) { refreshAssignees(); }
    }

    @FXML private void showQueue() { queueHandler.run(); }
    @FXML private void refreshDetails() { if (ticketId != null) { loadDetails(ticketId, null); } }

    private void loadDetails(int id, String notice) {
        if (loading || actionLoading || disposed) { return; }
        loading = true;
        detailErrorLabel.setText("");
        detailNoticeLabel.setText("");
        updateBusyState();
        loader.load(id, () -> disposed, data -> {
            loading = false;
            stale = false;
            selectedTicket = data.ticket();
            render(selectedTicket);
            messagesList.setItems(FXCollections.observableArrayList(data.messages().content()));
            detailContent.setVisible(true);
            detailContent.setManaged(true);
            showNotice(notice, false);
            updateBusyState();
        }, failure -> {
            loading = false;
            stale = detailContent.isVisible();
            var displayed = showFailure(detailErrorLabel, failure);
            if (displayed && stale) {
                detailErrorLabel.setText(detailErrorLabel.getText()
                        + " Displayed details may be out of date. Refresh before taking an action.");
            }
            updateBusyState();
        });
    }

    @FXML
    private void takeTicket() {
        if (selectedTicket == null || actionLoading || !confirm("Take ticket",
                "Take " + selectedTicket.ticketNumber() + "?",
                "The ticket will be assigned to you and moved to In progress.",
                "Take ticket", "Cancel", false)) {
            return;
        }
        mutate(ticketService.take(selectedTicket.id()), "Ticket assigned to you.");
    }

    @FXML
    private void beginWork() {
        if (selectedTicket != null && !actionLoading) {
            mutate(ticketService.beginWork(selectedTicket.id()), "Work started on this ticket.");
        }
    }

    @FXML
    private void reopenTicket() {
        if (selectedTicket == null || actionLoading || !confirm("Reopen ticket",
                "Reopen " + selectedTicket.ticketNumber() + "?",
                "The ticket will return to the open queue and its assignment and resolution will be cleared.",
                "Reopen ticket", "Cancel", false)) {
            return;
        }
        mutate(ticketService.reopen(selectedTicket.id()), "Ticket reopened and returned to the queue.");
    }

    @FXML
    private void cancelTicket() {
        if (selectedTicket == null || actionLoading || !confirm("Cancel ticket",
                "Cancel " + selectedTicket.ticketNumber() + "?",
                "Cancelled tickets cannot be reopened.", "Cancel ticket", "Keep ticket", true)) {
            return;
        }
        mutate(ticketService.cancel(selectedTicket.id()), "Ticket cancelled.");
    }

    @FXML
    private void changePriority() {
        if (selectedTicket == null || actionLoading || priorityField.getValue() == null
                || priorityField.getValue() == selectedTicket.priority()) {
            return;
        }
        mutate(ticketService.changePriority(selectedTicket.id(), priorityField.getValue()), "Priority updated.");
    }

    @FXML
    private void resolveTicket() {
        if (selectedTicket == null || actionLoading) {
            return;
        }
        var note = resolutionField.getText();
        if (note == null || note.isBlank()) {
            resolutionErrorLabel.setText("Enter a resolution note before resolving the ticket.");
            resolutionField.requestFocus();
            return;
        }
        if (note.trim().length() > 10_000) {
            resolutionErrorLabel.setText("Resolution note must contain at most 10,000 characters.");
            resolutionField.requestFocus();
            return;
        }
        if (!confirm("Resolve ticket", "Resolve " + selectedTicket.ticketNumber() + "?",
                "The requester will see the resolution note and can reopen the ticket.",
                "Resolve ticket", "Cancel", false)) {
            return;
        }
        mutate(ticketService.resolve(selectedTicket.id(), note), "Ticket resolved.");
    }

    @FXML
    private void addMessage() {
        if (selectedTicket == null || actionLoading) { return; }
        var error = TicketValidator.validateMessage(messageField.getText());
        if (error != null) {
            messageErrorLabel.setText(error);
            messageField.requestFocus();
            return;
        }
        actionLoading = true;
        detailErrorLabel.setText("");
        detailNoticeLabel.setText("");
        messageErrorLabel.setText("");
        updateBusyState();
        operations.run(ticketService.addMessage(
                        selectedTicket.id(), messageTypeField.getValue(), messageField.getText()),
                () -> disposed, message -> {
                    actionLoading = false;
                    messageField.clear();
                    messagesList.getItems().add(message);
                    messagesList.scrollTo(message);
                    showNotice(message.messageType().equals("INTERNAL_NOTE")
                            ? "Internal note added. It is visible only to IT staff."
                            : "Public comment posted.", true);
                    updateBusyState();
                }, failure -> {
                    actionLoading = false;
                    updateBusyState();
                    showFailure(messageErrorLabel, failure);
                });
    }

    @FXML
    private void refreshAssignees() {
        if (!isManager() || assigneesLoading || disposed) {
            return;
        }
        assigneesLoading = true;
        updateBusyState();
        operations.run(managerService.technicians(), () -> disposed, users -> {
            assigneesLoading = false;
            assigneeField.setItems(FXCollections.observableArrayList(users));
            updateBusyState();
        }, failure -> {
            assigneesLoading = false;
            updateBusyState();
            showFailure(detailErrorLabel, failure);
        });
    }

    @FXML
    private void assignTicket() {
        if (selectedTicket == null || actionLoading || assigneeField.getValue() == null) {
            return;
        }
        mutate(managerService.assign(selectedTicket.id(), assigneeField.getValue().id()), "Assignment updated.");
    }

    private void mutate(CompletionStage<Ticket> operation, String notice) {
        if (selectedTicket == null || actionLoading) {
            return;
        }
        actionLoading = true;
        detailErrorLabel.setText("");
        detailNoticeLabel.setText("");
        updateBusyState();
        operations.run(operation, () -> disposed, ticket -> {
            actionLoading = false;
            selectedTicket = ticket;
            render(ticket);
            showNotice(notice, true);
            updateBusyState();
            queueRefreshHandler.run();
        }, failure -> {
            actionLoading = false;
            updateBusyState();
            if (isConflict(failure) && selectedTicket != null) {
                loadDetails(selectedTicket.id(), "This ticket changed elsewhere. We reloaded the latest details.");
            } else {
                showFailure(detailErrorLabel, failure);
            }
        });
    }

    private void render(Ticket ticket) {
        renderer.render(ticket, session.current().orElseThrow().user().id(), isManager(),
                TechnicianTicketDetailController::formatDate,
                new TechnicianTicketDetailRenderer.DetailView(detailNumberLabel, detailStatusLabel, detailSubjectLabel,
                        detailDescriptionLabel, detailCategoryLabel, detailPriorityLabel, detailRequesterLabel,
                        detailAssigneeLabel, detailCreatedLabel, priorityField, resolutionBox, resolutionLabel,
                        takeButton, beginWorkButton, reopenButton, resolveBox, cancelButton, resolutionField,
                        resolutionErrorLabel));
    }

    private void updateBusyState() {
        var busy = loading || actionLoading;
        detailContent.setDisable(busy || stale);
        detailProgress.setVisible(busy || assigneesLoading);
        detailProgress.setManaged(detailProgress.isVisible());
        detailBusyLabel.setText(actionLoading ? "Saving changes…"
                : assigneesLoading ? "Loading assignable users…"
                : selectedTicket == null ? "Loading ticket…" : "Refreshing ticket…");
        detailBusyLabel.setVisible(detailProgress.isVisible());
        detailBusyLabel.setManaged(detailBusyLabel.isVisible());
        refreshDetailButton.setDisable(busy);
        takeButton.setDisable(actionLoading);
        beginWorkButton.setDisable(actionLoading);
        cancelButton.setDisable(busy);
        reopenButton.setDisable(actionLoading);
        priorityField.setDisable(actionLoading);
        changePriorityButton.setDisable(actionLoading || selectedTicket == null
                || priorityField.getValue() == null || priorityField.getValue() == selectedTicket.priority());
        resolutionField.setDisable(actionLoading);
        resolveButton.setDisable(actionLoading);
        messageTypeField.setDisable(actionLoading);
        messageField.setDisable(actionLoading);
        addMessageButton.setDisable(actionLoading);
        assigneeField.setDisable(busy || assigneesLoading);
        refreshAssigneesButton.setDisable(busy || assigneesLoading);
        assignButton.setDisable(busy || assigneesLoading || selectedTicket == null
                || assigneeField.getValue() == null || selectedTicket.status() == TicketStatus.RESOLVED
                || selectedTicket.status() == TicketStatus.CANCELLED);
        busyHandler.accept(busy);
    }

    private boolean confirm(String title, String header, String content,
                            String confirmText, String cancelText, boolean destructive) {
        var confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        var cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        var alert = new Alert(Alert.AlertType.CONFIRMATION, content, cancelButton, confirmButton);
        alert.setTitle(title);
        alert.setHeaderText(header);
        var dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("technician-confirmation-dialog");
        var stylesheet = getClass().getResource("/resolveit/frontend/styles/app.css");
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }
        ((Button) dialogPane.lookupButton(confirmButton)).getStyleClass()
                .add(destructive ? "danger-button" : "primary-button");
        ((Button) dialogPane.lookupButton(cancelButton)).getStyleClass().add("secondary-button");
        return alert.showAndWait().filter(confirmButton::equals).isPresent();
    }

    private boolean showFailure(Label target, Throwable problem) {
        var cause = unwrap(problem);
        if (cause instanceof TicketFailure failure) {
            if (failure.kind() == TicketFailure.Kind.UNAUTHORIZED) {
                session.clear();
                navigator.showLogin();
                return false;
            }
            target.setText(failure.getMessage());
        } else {
            target.setText("The request could not be completed. Please try again.");
        }
        return true;
    }

    private boolean isConflict(Throwable problem) {
        return unwrap(problem) instanceof TicketFailure failure
                && failure.kind() == TicketFailure.Kind.CONFLICT;
    }

    private Throwable unwrap(Throwable problem) {
        var current = problem;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void showNotice(String message, boolean success) {
        detailNoticeBox.getStyleClass().removeAll("success-message", "information-message");
        detailNoticeBox.getStyleClass().add(success ? "success-message" : "information-message");
        detailNoticeMark.setVisible(success);
        detailNoticeMark.setManaged(success);
        detailNoticeLabel.setText(message == null ? "" : message);
    }

    private boolean isManager() {
        return session.current().orElseThrow().user().role() == Role.MANAGER;
    }

    private void updateMessageCopy(MessageType type) {
        var internal = type == MessageType.INTERNAL_NOTE;
        messageField.setPromptText(internal
                ? "Write a note visible only to IT staff…" : "Write an update the requester can see…");
        addMessageButton.setText(internal ? "_Add internal note" : "_Post public comment");
    }

    private void bindManaged(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    private void bindManaged(HBox box, Label label) {
        box.visibleProperty().bind(label.textProperty().isNotEmpty());
        box.managedProperty().bind(box.visibleProperty());
    }

    private static String formatDate(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        try {
            return DATE_FORMAT.format(Instant.parse(value).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private static final class MessageCell extends ListCell<TicketMessage> {
        @Override
        protected void updateItem(TicketMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().remove("internal-note-cell");
                return;
            }
            var internal = "INTERNAL_NOTE".equals(item.messageType());
            var author = new Label(item.authorUsername() + "  ·  " + formatDate(item.createdAt())
                    + (internal ? "  ·  INTERNAL NOTE" : ""));
            author.getStyleClass().add("comment-author");
            var message = new Label(item.message());
            message.setWrapText(true);
            message.getStyleClass().add("comment-message");
            var box = new VBox(5, author, message);
            box.getStyleClass().add(internal ? "internal-note-card" : "comment-card");
            setText(null);
            setGraphic(box);
            getStyleClass().remove("internal-note-cell");
            if (internal) {
                getStyleClass().add("internal-note-cell");
            }
        }
    }
}
