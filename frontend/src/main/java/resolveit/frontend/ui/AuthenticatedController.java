package resolveit.frontend.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.EmployeeTicketService;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketCategory;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.ticket.TicketMessage;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;
import resolveit.frontend.ticket.TicketStatus;
import resolveit.frontend.ticket.TicketValidator;

public final class AuthenticatedController implements ViewLifecycle {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final SessionState session;
    private final EmployeeTicketService ticketService;
    private final Navigator navigator;
    private final Set<CompletableFuture<?>> inFlight = new HashSet<>();
    private Ticket selectedTicket;
    private boolean disposed;
    private boolean listLoading;
    private boolean detailLoading;
    private boolean actionLoading;
    private int currentTicketPage;
    private int ticketPageCount;

    @FXML private Label avatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button myTicketsNavButton;
    @FXML private Button submitNavButton;
    @FXML private StackPane myTicketsPage;
    @FXML private StackPane submitTicketPage;
    @FXML private StackPane ticketDetailsPage;

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button refreshTicketsButton;
    @FXML private ProgressIndicator ticketsProgress;
    @FXML private Label ticketsErrorLabel;
    @FXML private Label ticketCountLabel;
    @FXML private Label ticketPageLabel;
    @FXML private Button previousTicketsButton;
    @FXML private Button nextTicketsButton;
    @FXML private TableView<Ticket> ticketsTable;
    @FXML private TableColumn<Ticket, String> numberColumn;
    @FXML private TableColumn<Ticket, String> subjectColumn;
    @FXML private TableColumn<Ticket, String> categoryColumn;
    @FXML private TableColumn<Ticket, String> priorityColumn;
    @FXML private TableColumn<Ticket, String> statusColumn;
    @FXML private TableColumn<Ticket, String> updatedColumn;

    @FXML private TextField createSubjectField;
    @FXML private TextArea createDescriptionField;
    @FXML private ComboBox<TicketCategory> createCategoryField;
    @FXML private ComboBox<TicketPriority> createPriorityField;
    @FXML private Label createSubjectError;
    @FXML private Label createDescriptionError;
    @FXML private Label createCategoryError;
    @FXML private Label createPriorityError;
    @FXML private Label createFormError;
    @FXML private Label createSuccessLabel;
    @FXML private Button createTicketButton;
    @FXML private ProgressIndicator createProgress;

    @FXML private Label detailNumberLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailErrorLabel;
    @FXML private ProgressIndicator detailProgress;
    @FXML private VBox detailContent;
    @FXML private VBox detailReadOnly;
    @FXML private Label detailSubjectLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailPriorityLabel;
    @FXML private Label detailAssigneeLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private VBox resolutionBox;
    @FXML private Label resolutionLabel;
    @FXML private VBox editForm;
    @FXML private TextField editSubjectField;
    @FXML private TextArea editDescriptionField;
    @FXML private ComboBox<TicketCategory> editCategoryField;
    @FXML private ComboBox<TicketPriority> editPriorityField;
    @FXML private Label editValidationLabel;
    @FXML private Button editButton;
    @FXML private Button saveEditButton;
    @FXML private Button cancelActionButton;
    @FXML private Button reopenActionButton;
    @FXML private Button refreshDetailButton;
    @FXML private ListView<TicketMessage> messagesList;
    @FXML private TextArea commentField;
    @FXML private Label commentErrorLabel;
    @FXML private Button addCommentButton;

    public AuthenticatedController(SessionState session, EmployeeTicketService ticketService, Navigator navigator) {
        this.session = session;
        this.ticketService = ticketService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        var user = session.current().orElseThrow().user();
        userNameLabel.setText(user.username());
        userRoleLabel.setText(user.role().displayName());
        avatarLabel.setText(initials(user.username()));

        statusFilter.setItems(FXCollections.observableArrayList(
                "All statuses", "Open", "In progress", "Resolved", "Cancelled"));
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.valueProperty().addListener((ignored, oldValue, newValue) -> {
            currentTicketPage = 0;
            refreshTickets();
        });
        createCategoryField.setItems(FXCollections.observableArrayList(TicketCategory.values()));
        createPriorityField.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        createCategoryField.setConverter(enumConverter(TicketCategory::displayName));
        createPriorityField.setConverter(enumConverter(TicketPriority::displayName));
        createPriorityField.setValue(TicketPriority.MEDIUM);
        editCategoryField.setItems(FXCollections.observableArrayList(TicketCategory.values()));
        editPriorityField.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        editCategoryField.setConverter(enumConverter(TicketCategory::displayName));
        editPriorityField.setConverter(enumConverter(TicketPriority::displayName));

        numberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ticketNumber()));
        subjectColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().subject()));
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().category().displayName()));
        priorityColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().priority().displayName()));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().status().displayName()));
        updatedColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().updatedAt())));
        ticketsTable.setRowFactory(ignored -> ticketRow());
        ticketsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) openSelectedTicket();
        });

        messagesList.setCellFactory(ignored -> new MessageCell());
        bindManaged(ticketsErrorLabel);
        bindManaged(createSubjectError);
        bindManaged(createDescriptionError);
        bindManaged(createCategoryError);
        bindManaged(createPriorityError);
        bindManaged(createFormError);
        bindManaged(createSuccessLabel);
        bindManaged(detailErrorLabel);
        bindManaged(editValidationLabel);
        bindManaged(commentErrorLabel);
        createSubjectField.textProperty().addListener((ignored, oldValue, newValue) -> clearCreateFeedback());
        createDescriptionField.textProperty().addListener((ignored, oldValue, newValue) -> clearCreateFeedback());
        commentField.textProperty().addListener((ignored, oldValue, newValue) -> commentErrorLabel.setText(""));
    }

    @Override
    public void onShown() {
        showMyTickets();
    }

    @FXML
    private void showMyTickets() {
        showPage(myTicketsPage);
        setActiveNavigation(myTicketsNavButton);
        refreshTickets();
    }

    @FXML
    private void showSubmitTicket() {
        showPage(submitTicketPage);
        setActiveNavigation(submitNavButton);
        Platform.runLater(createSubjectField::requestFocus);
    }

    @FXML
    private void refreshTickets() {
        if (listLoading || disposed) return;
        listLoading = true;
        ticketsErrorLabel.setText("");
        updateBusyState();
        run(ticketService.list(selectedStatus(), currentTicketPage), page -> {
            ticketsTable.setItems(FXCollections.observableArrayList(page.content()));
            ticketCountLabel.setText(page.totalElements() == 1 ? "1 ticket" : page.totalElements() + " tickets");
            ticketPageCount = page.totalPages();
            currentTicketPage = page.page();
            ticketPageLabel.setText(ticketPageCount == 0 ? "Page 0 of 0"
                    : "Page " + (currentTicketPage + 1) + " of " + ticketPageCount);
            listLoading = false;
            updateBusyState();
        }, failure -> {
            listLoading = false;
            updateBusyState();
            showFailure(ticketsErrorLabel, failure);
        });
    }

    @FXML
    private void previousTicketPage() {
        if (currentTicketPage > 0 && !listLoading) {
            currentTicketPage--;
            refreshTickets();
        }
    }

    @FXML
    private void nextTicketPage() {
        if (currentTicketPage + 1 < ticketPageCount && !listLoading) {
            currentTicketPage++;
            refreshTickets();
        }
    }

    @FXML
    private void openSelectedTicket() {
        var ticket = ticketsTable.getSelectionModel().getSelectedItem();
        if (ticket != null) openTicket(ticket.id());
    }

    private void openTicket(int ticketId) {
        showPage(ticketDetailsPage);
        setActiveNavigation(null);
        selectedTicket = null;
        detailNumberLabel.setText("Ticket details");
        detailContent.setVisible(false);
        detailContent.setManaged(false);
        loadDetails(ticketId);
    }

    @FXML
    private void refreshDetails() {
        if (selectedTicket != null) loadDetails(selectedTicket.id());
    }

    private void loadDetails(int ticketId) {
        loadDetails(ticketId, null);
    }

    private void loadDetails(int ticketId, String noticeAfterLoad) {
        if (detailLoading || actionLoading || disposed) return;
        detailLoading = true;
        detailErrorLabel.setText("");
        updateBusyState();
        var ticketFuture = ticketService.get(ticketId).toCompletableFuture();
        var messagesFuture = ticketService.messages(ticketId).toCompletableFuture();
        var combined = ticketFuture.thenCombine(messagesFuture, DetailData::new);
        track(ticketFuture);
        track(messagesFuture);
        track(combined);
        combined.whenComplete((data, failure) -> Platform.runLater(() -> {
            inFlight.remove(ticketFuture);
            inFlight.remove(messagesFuture);
            inFlight.remove(combined);
            if (disposed) return;
            detailLoading = false;
            updateBusyState();
            if (failure != null) {
                showFailure(detailErrorLabel, failure);
                return;
            }
            selectedTicket = data.ticket();
            renderTicket(data.ticket());
            messagesList.setItems(FXCollections.observableArrayList(data.messages().content()));
            detailContent.setVisible(true);
            detailContent.setManaged(true);
            if (noticeAfterLoad != null) detailErrorLabel.setText(noticeAfterLoad);
        }));
    }

    @FXML
    private void submitTicket() {
        if (actionLoading) return;
        clearCreateFeedback();
        var validation = TicketValidator.validateTicket(createSubjectField.getText(), createDescriptionField.getText(),
                createCategoryField.getValue(), createPriorityField.getValue());
        showCreateValidation(validation);
        if (!validation.isValid()) return;
        actionLoading = true;
        updateBusyState();
        var request = new CreateTicket(createSubjectField.getText().trim(), createDescriptionField.getText().trim(),
                createCategoryField.getValue(), createPriorityField.getValue());
        run(ticketService.create(request), ticket -> {
            actionLoading = false;
            updateBusyState();
            clearCreateForm();
            createSuccessLabel.setText(ticket.ticketNumber() + " was submitted successfully.");
        }, failure -> {
            actionLoading = false;
            updateBusyState();
            showFailure(createFormError, failure);
        });
    }

    @FXML
    private void beginEdit() {
        if (selectedTicket == null || !selectedTicket.isEditableByRequester()) return;
        editSubjectField.setText(selectedTicket.subject());
        editDescriptionField.setText(selectedTicket.description());
        editCategoryField.setValue(selectedTicket.category());
        editPriorityField.setValue(selectedTicket.priority());
        editValidationLabel.setText("");
        setEditing(true);
        Platform.runLater(editSubjectField::requestFocus);
    }

    @FXML
    private void discardEdit() {
        setEditing(false);
    }

    @FXML
    private void saveEdit() {
        if (selectedTicket == null || actionLoading) return;
        var validation = TicketValidator.validateTicket(editSubjectField.getText(), editDescriptionField.getText(),
                editCategoryField.getValue(), editPriorityField.getValue());
        if (!validation.isValid()) {
            editValidationLabel.setText(firstError(validation));
            return;
        }
        actionLoading = true;
        updateBusyState();
        var request = new UpdateTicket(editSubjectField.getText().trim(), editDescriptionField.getText().trim(),
                editCategoryField.getValue(), editPriorityField.getValue(), selectedTicket.version());
        run(ticketService.update(selectedTicket.id(), request), ticket -> {
            actionLoading = false;
            selectedTicket = ticket;
            setEditing(false);
            renderTicket(ticket);
            updateBusyState();
            refreshTicketsInBackground();
        }, failure -> {
            actionLoading = false;
            updateBusyState();
            if (isConflict(failure)) {
                loadDetails(selectedTicket.id(), conflictNotice());
            } else {
                showFailure(detailErrorLabel, failure);
            }
        });
    }

    @FXML
    private void cancelTicket() {
        if (selectedTicket == null || actionLoading || !confirm(
                "Cancel ticket", "Cancel " + selectedTicket.ticketNumber() + "?",
                "This ticket cannot be reopened after it is cancelled.")) return;
        mutateTicket(ticketService.cancel(selectedTicket.id()));
    }

    @FXML
    private void reopenTicket() {
        if (selectedTicket == null || actionLoading || !confirm(
                "Reopen ticket", "Reopen " + selectedTicket.ticketNumber() + "?",
                "The ticket will return to the open queue and its previous assignment and resolution will be cleared.")) return;
        mutateTicket(ticketService.reopen(selectedTicket.id()));
    }

    @FXML
    private void addComment() {
        if (selectedTicket == null || actionLoading) return;
        var error = TicketValidator.validateMessage(commentField.getText());
        if (error != null) {
            commentErrorLabel.setText(error);
            return;
        }
        actionLoading = true;
        updateBusyState();
        run(ticketService.addComment(selectedTicket.id(), commentField.getText()), message -> {
            actionLoading = false;
            commentField.clear();
            messagesList.getItems().add(message);
            messagesList.scrollTo(message);
            updateBusyState();
        }, failure -> {
            actionLoading = false;
            updateBusyState();
            showFailure(commentErrorLabel, failure);
        });
    }

    private void mutateTicket(java.util.concurrent.CompletionStage<Ticket> operation) {
        actionLoading = true;
        detailErrorLabel.setText("");
        updateBusyState();
        run(operation, ticket -> {
            actionLoading = false;
            selectedTicket = ticket;
            renderTicket(ticket);
            updateBusyState();
            refreshTicketsInBackground();
        }, failure -> {
            actionLoading = false;
            updateBusyState();
            if (isConflict(failure) && selectedTicket != null) {
                loadDetails(selectedTicket.id(), conflictNotice());
            } else {
                showFailure(detailErrorLabel, failure);
            }
        });
    }

    private void renderTicket(Ticket ticket) {
        detailNumberLabel.setText(ticket.ticketNumber());
        detailStatusLabel.setText(ticket.status().displayName());
        detailStatusLabel.getStyleClass().removeIf(name -> name.startsWith("status-"));
        detailStatusLabel.getStyleClass().add("status-" + ticket.status().name().toLowerCase().replace('_', '-'));
        detailSubjectLabel.setText(ticket.subject());
        detailDescriptionLabel.setText(ticket.description());
        detailCategoryLabel.setText(ticket.category().displayName());
        detailPriorityLabel.setText(ticket.priority().displayName());
        detailAssigneeLabel.setText(ticket.assignedToUsername() == null ? "Not assigned yet" : ticket.assignedToUsername());
        detailCreatedLabel.setText(formatDate(ticket.createdAt()));
        resolutionBox.setVisible(ticket.resolutionNote() != null && !ticket.resolutionNote().isBlank());
        resolutionBox.setManaged(resolutionBox.isVisible());
        resolutionLabel.setText(ticket.resolutionNote() == null ? "" : ticket.resolutionNote());
        editButton.setVisible(ticket.isEditableByRequester());
        editButton.setManaged(editButton.isVisible());
        cancelActionButton.setVisible(ticket.isCancellableByRequester());
        cancelActionButton.setManaged(cancelActionButton.isVisible());
        reopenActionButton.setVisible(ticket.isReopenableByRequester());
        reopenActionButton.setManaged(reopenActionButton.isVisible());
        setEditing(false);
    }

    private void setEditing(boolean editing) {
        editForm.setVisible(editing);
        editForm.setManaged(editing);
        detailReadOnly.setVisible(!editing);
        detailReadOnly.setManaged(!editing);
        editButton.setDisable(editing || actionLoading);
    }

    private void updateBusyState() {
        ticketsProgress.setVisible(listLoading);
        ticketsProgress.setManaged(listLoading);
        refreshTicketsButton.setDisable(listLoading);
        statusFilter.setDisable(listLoading);
        previousTicketsButton.setDisable(listLoading || currentTicketPage <= 0);
        nextTicketsButton.setDisable(listLoading || currentTicketPage + 1 >= ticketPageCount);
        createProgress.setVisible(actionLoading && submitTicketPage.isVisible());
        createProgress.setManaged(createProgress.isVisible());
        createTicketButton.setDisable(actionLoading);
        detailProgress.setVisible(detailLoading || actionLoading);
        detailProgress.setManaged(detailProgress.isVisible());
        refreshDetailButton.setDisable(detailLoading || actionLoading);
        editButton.setDisable(detailLoading || actionLoading || editForm.isVisible());
        saveEditButton.setDisable(actionLoading);
        cancelActionButton.setDisable(actionLoading);
        reopenActionButton.setDisable(actionLoading);
        addCommentButton.setDisable(actionLoading);
        commentField.setDisable(actionLoading);
    }

    private void refreshTicketsInBackground() {
        if (!listLoading) refreshTickets();
    }

    private void showPage(StackPane page) {
        for (var candidate : new StackPane[] { myTicketsPage, submitTicketPage, ticketDetailsPage }) {
            candidate.setVisible(candidate == page);
            candidate.setManaged(candidate == page);
        }
        updateBusyState();
    }

    private void setActiveNavigation(Button active) {
        for (var button : new Button[] { myTicketsNavButton, submitNavButton }) {
            button.getStyleClass().remove("nav-button-active");
            if (button == active) button.getStyleClass().add("nav-button-active");
        }
    }

    private TicketStatus selectedStatus() {
        return switch (statusFilter.getSelectionModel().getSelectedIndex()) {
            case 1 -> TicketStatus.OPEN;
            case 2 -> TicketStatus.IN_PROGRESS;
            case 3 -> TicketStatus.RESOLVED;
            case 4 -> TicketStatus.CANCELLED;
            default -> null;
        };
    }

    private TableRow<Ticket> ticketRow() {
        var row = new TableRow<Ticket>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) openTicket(row.getItem().id());
        });
        return row;
    }

    private void showCreateValidation(TicketValidator.Validation validation) {
        createSubjectError.setText(valueOrEmpty(validation.subjectError()));
        createDescriptionError.setText(valueOrEmpty(validation.descriptionError()));
        createCategoryError.setText(valueOrEmpty(validation.categoryError()));
        createPriorityError.setText(valueOrEmpty(validation.priorityError()));
    }

    private String firstError(TicketValidator.Validation validation) {
        if (validation.subjectError() != null) return validation.subjectError();
        if (validation.descriptionError() != null) return validation.descriptionError();
        if (validation.categoryError() != null) return validation.categoryError();
        return validation.priorityError();
    }

    private void clearCreateFeedback() {
        createSubjectError.setText("");
        createDescriptionError.setText("");
        createCategoryError.setText("");
        createPriorityError.setText("");
        createFormError.setText("");
        createSuccessLabel.setText("");
    }

    private void clearCreateForm() {
        createSubjectField.clear();
        createDescriptionField.clear();
        createCategoryField.setValue(null);
        createPriorityField.setValue(TicketPriority.MEDIUM);
    }

    private boolean confirm(String title, String header, String content) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private <T> void run(java.util.concurrent.CompletionStage<T> operation,
                         Consumer<T> success, Consumer<Throwable> failure) {
        var future = operation.toCompletableFuture();
        track(future);
        future.whenComplete((result, problem) -> Platform.runLater(() -> {
            inFlight.remove(future);
            if (disposed) return;
            if (problem == null) success.accept(result); else failure.accept(problem);
        }));
    }

    private void track(CompletableFuture<?> future) {
        inFlight.add(future);
    }

    private void showFailure(Label target, Throwable problem) {
        var cause = unwrap(problem);
        if (cause instanceof TicketFailure ticketFailure) {
            if (ticketFailure.kind() == TicketFailure.Kind.UNAUTHORIZED) {
                session.clear();
                navigator.showLogin();
                return;
            }
            target.setText(isConflict(ticketFailure)
                    ? "This ticket changed since you opened it. The latest details have been loaded."
                    : ticketFailure.getMessage());
        } else {
            target.setText("The request could not be completed. Please try again.");
        }
    }

    private boolean isConflict(Throwable problem) {
        var cause = unwrap(problem);
        return cause instanceof TicketFailure failure && failure.kind() == TicketFailure.Kind.CONFLICT;
    }

    private String conflictNotice() {
        return "This ticket changed since you opened it. The latest details have been loaded.";
    }

    private Throwable unwrap(Throwable problem) {
        var current = problem;
        while (current instanceof CompletionException && current.getCause() != null) current = current.getCause();
        return current;
    }

    private void bindManaged(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    private String formatDate(String value) {
        if (value == null || value.isBlank()) return "—";
        try {
            return DATE_FORMAT.format(Instant.parse(value).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private String initials(String username) {
        if (username == null || username.isBlank()) return "RI";
        var parts = username.trim().split("[._\\-\\s]+", 2);
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String valueOrEmpty(String value) { return value == null ? "" : value; }

    private <T> StringConverter<T> enumConverter(java.util.function.Function<T, String> displayName) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : displayName.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException(); }
        };
    }

    @FXML
    private void logout() {
        session.clear();
        navigator.showLogin();
    }

    @Override
    public void dispose() {
        disposed = true;
        for (var future : Set.copyOf(inFlight)) future.cancel(true);
        inFlight.clear();
    }

    private record DetailData(Ticket ticket, PageResponse<TicketMessage> messages) {}

    private final class MessageCell extends ListCell<TicketMessage> {
        @Override
        protected void updateItem(TicketMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            var author = new Label(item.authorUsername() + "  ·  " + formatDate(item.createdAt()));
            author.getStyleClass().add("comment-author");
            var message = new Label(item.message());
            message.setWrapText(true);
            message.getStyleClass().add("comment-message");
            var box = new VBox(5, author, message);
            box.getStyleClass().add("comment-card");
            setText(null);
            setGraphic(box);
        }
    }
}
