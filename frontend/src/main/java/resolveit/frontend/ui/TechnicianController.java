package resolveit.frontend.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import resolveit.frontend.navigation.Navigator;
import resolveit.frontend.navigation.ViewLifecycle;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.PageResponse;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TechnicianTicketService.AssignmentFilter;
import resolveit.frontend.ticket.TechnicianTicketService.MessageType;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.ticket.TicketMessage;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketStatus;
import resolveit.frontend.ticket.TicketValidator;
import resolveit.frontend.user.ManagerService;

/**
 * Controls the technician and manager workspace: the ticket queue, ticket detail
 * with support actions (take, begin work, resolve, priority, notes), and manager
 * assignment. Runs operations asynchronously and marshals results to the JavaFX
 * thread, tracks in-flight requests for cancellation on disposal, gates the UI
 * with per-area loading flags, and marks the queue or detail stale when a refresh
 * fails so the user is not shown outdated data as current.
 */
public final class TechnicianController implements ViewLifecycle {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final SessionState session;
    private final TechnicianTicketService ticketService;
    private final ManagerService managerService;
    private final Navigator navigator;
    private final Set<CompletableFuture<?>> inFlight = new HashSet<>();
    private Ticket selectedTicket;
    private Integer detailTicketId;
    private boolean disposed;
    private boolean listLoading;
    private boolean queueStale;
    private boolean detailLoading;
    private boolean detailStale;
    private boolean actionLoading;
    private int currentPage;
    private int pageCount;

    @FXML private Label workspaceLabel, queueHeading, queueDescription;
    @FXML private Button usersNavButton, requestsNavButton, logoutButton;
    @FXML private Button cancelButton, assignButton, refreshAssigneesButton;
    @FXML private VBox assignmentBox;
    @FXML private ComboBox<resolveit.frontend.model.User> assigneeField;
    private boolean assigneesLoading;
    private boolean isManager() {
        return session.current().orElseThrow().user().role() == resolveit.frontend.model.Role.MANAGER;
    }
    @FXML private void showRequests() { navigator.showRequesterWorkspace(); }
    @FXML private void showUsers() { navigator.showUsers(); }
    @FXML private void refreshAssignees() {
        if (assigneesLoading || disposed) {
            return;
        }
        assigneesLoading = true;
        updateBusyState();
        run(managerService.technicians(), users -> {
            assigneesLoading = false;
            assigneeField.setItems(FXCollections.observableArrayList(users));
            updateBusyState();
        }, failure -> {
            assigneesLoading = false;
            updateBusyState();
            showFailure(detailErrorLabel, failure);
        });
    }
    @FXML private void assignTicket() {
        if (selectedTicket == null || actionLoading || assigneeField.getValue() == null) {
            return;
        }
        mutate(managerService.assign(selectedTicket.id(), assigneeField.getValue().id()), "Assignment updated.");
    }
    @FXML private void cancelTicket() {
        if (selectedTicket == null || actionLoading
                || !confirm("Cancel ticket", "Cancel " + selectedTicket.ticketNumber() + "?",
                        "Cancelled tickets cannot be reopened.", "Cancel ticket", "Keep ticket", true)) {
            return;
        }
        mutate(ticketService.cancel(selectedTicket.id()), "Ticket cancelled.");
    }

    @FXML private Label avatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button queueNavButton;
    @FXML private StackPane queuePage;
    @FXML private StackPane detailPage;
    @FXML private ComboBox<TicketStatus> statusFilter;
    @FXML private ComboBox<TicketPriority> priorityFilter;
    @FXML private ComboBox<AssignmentFilter> assignmentFilter;
    @FXML private Label statusFilterLabel, priorityFilterLabel, assignmentFilterLabel;
    @FXML private Button refreshQueueButton;
    @FXML private ProgressIndicator queueProgress;
    @FXML private Label queueBusyLabel;
    @FXML private Label queueErrorLabel;
    @FXML private Label queueEmptyLabel;
    @FXML private Label ticketCountLabel;
    @FXML private Label pageLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private TableView<Ticket> ticketsTable;
    @FXML private TableColumn<Ticket, String> numberColumn;
    @FXML private TableColumn<Ticket, String> subjectColumn;
    @FXML private TableColumn<Ticket, String> requesterColumn;
    @FXML private TableColumn<Ticket, String> priorityColumn;
    @FXML private TableColumn<Ticket, String> statusColumn;
    @FXML private TableColumn<Ticket, String> assigneeColumn;
    @FXML private TableColumn<Ticket, String> updatedColumn;

    @FXML private Label detailNumberLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailErrorLabel;
    @FXML private Label detailNoticeLabel;
    @FXML private HBox detailNoticeBox;
    @FXML private StackPane detailNoticeMark;
    @FXML private ProgressIndicator detailProgress;
    @FXML private Label detailBusyLabel;
    @FXML private Button refreshDetailButton;
    @FXML private VBox detailContent;
    @FXML private Label detailSubjectLabel;
    @FXML private Label detailDescriptionLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailPriorityLabel;
    @FXML private Label detailRequesterLabel;
    @FXML private Label detailAssigneeLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private VBox resolutionBox;
    @FXML private Label resolutionLabel;
    @FXML private Button takeButton;
    @FXML private Button beginWorkButton;
    @FXML private Button reopenButton;
    @FXML private ComboBox<TicketPriority> priorityField;
    @FXML private Button changePriorityButton;
    @FXML private VBox resolveBox;
    @FXML private TextArea resolutionField;
    @FXML private Label resolutionErrorLabel;
    @FXML private Button resolveButton;
    @FXML private ListView<TicketMessage> messagesList;
    @FXML private ComboBox<MessageType> messageTypeField;
    @FXML private TextArea messageField;
    @FXML private Label messageErrorLabel;
    @FXML private Button addMessageButton;
    @FXML private Label assigneeFieldLabel, priorityFieldLabel, resolutionFieldLabel;
    @FXML private Label messageTypeFieldLabel, messageFieldLabel;

    /**
     * Creates the technician/manager workspace controller.
     *
     * @param session current in-memory session
     * @param ticketService technician ticket operations
     * @param managerService manager operations used for assignment
     * @param navigator navigator used for sign-out and view switches
     */
    public TechnicianController(SessionState session, TechnicianTicketService ticketService,
                                ManagerService managerService, Navigator navigator) {
        this.session = session;
        this.ticketService = ticketService;
        this.managerService = managerService;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        var user = session.current().orElseThrow().user();
        userNameLabel.setText(user.username());
        userRoleLabel.setText(user.role().displayName());
        avatarLabel.setText(initials(user.username()));

        statusFilterLabel.setLabelFor(statusFilter);
        priorityFilterLabel.setLabelFor(priorityFilter);
        assignmentFilterLabel.setLabelFor(assignmentFilter);
        assigneeFieldLabel.setLabelFor(assigneeField);
        priorityFieldLabel.setLabelFor(priorityField);
        resolutionFieldLabel.setLabelFor(resolutionField);
        messageTypeFieldLabel.setLabelFor(messageTypeField);
        messageFieldLabel.setLabelFor(messageField);

        statusFilter.setItems(FXCollections.observableArrayList(
                TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED));
        statusFilter.getItems().addFirst(null);
        if (isManager()) {
            statusFilter.getItems().add(TicketStatus.CANCELLED);
        }
        usersNavButton.setVisible(isManager());
        usersNavButton.setManaged(isManager());
        assignmentBox.setVisible(isManager());
        assignmentBox.setManaged(isManager());
        if (isManager()) {
            workspaceLabel.setText("Manager workspace");
            queueHeading.setText("All Tickets");
            queueDescription.setText(
                    "Review all requests, including resolved and cancelled tickets, and manage assignments.");
            queueNavButton.setText("All _Tickets");
            ticketsTable.setAccessibleText("All tickets");
        }
        assigneeField.setConverter(nullableConverter(
                "Choose an active technician or manager",
                candidate -> candidate.username() + " (" + candidate.role() + ")"));
        assigneeField.valueProperty().addListener((o, a, b) -> updateBusyState());
        priorityFilter.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        priorityFilter.getItems().addFirst(null);
        assignmentFilter.setItems(FXCollections.observableArrayList(AssignmentFilter.values()));
        statusFilter.setConverter(nullableConverter("All statuses", TicketStatus::displayName));
        priorityFilter.setConverter(nullableConverter("All priorities", TicketPriority::displayName));
        assignmentFilter.setConverter(nullableConverter(
                "Any assignment",
                assignment -> assignment == AssignmentFilter.ALL
                        ? "Any assignment" : assignment.displayName()));
        assignmentFilter.setValue(AssignmentFilter.ALL);
        addFilterListener(statusFilter);
        addFilterListener(priorityFilter);
        addFilterListener(assignmentFilter);

        priorityField.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        priorityField.setConverter(nullableConverter("", TicketPriority::displayName));
        messageTypeField.setItems(FXCollections.observableArrayList(MessageType.values()));
        messageTypeField.setConverter(nullableConverter("", MessageType::displayName));
        messageTypeField.setValue(MessageType.PUBLIC_COMMENT);
        messageTypeField.valueProperty().addListener(
                (ignored, oldValue, newValue) -> updateMessageComposerCopy(newValue));
        updateMessageComposerCopy(messageTypeField.getValue());

        numberColumn.setCellValueFactory(cell -> text(cell.getValue().ticketNumber()));
        subjectColumn.setCellValueFactory(cell -> text(cell.getValue().subject()));
        requesterColumn.setCellValueFactory(cell -> text(cell.getValue().requesterUsername()));
        priorityColumn.setCellValueFactory(cell -> text(cell.getValue().priority().displayName()));
        statusColumn.setCellValueFactory(cell -> text(cell.getValue().status().displayName()));
        assigneeColumn.setCellValueFactory(cell -> text(cell.getValue().assignedToUsername() == null
                ? "Unassigned" : cell.getValue().assignedToUsername()));
        updatedColumn.setCellValueFactory(cell -> text(formatDate(cell.getValue().updatedAt())));
        configureQueueCellDiscovery();
        ticketsTable.widthProperty().addListener((ignored, oldWidth, newWidth) ->
                resizeQueueColumns(newWidth.doubleValue()));
        ticketsTable.setRowFactory(ignored -> ticketRow());
        ticketsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openSelectedTicket();
            }
        });
        messagesList.setCellFactory(ignored -> new MessageCell());

        bindManaged(queueErrorLabel);
        bindManaged(detailErrorLabel);
        bindManaged(detailNoticeBox, detailNoticeLabel);
        bindManaged(resolutionErrorLabel);
        bindManaged(messageErrorLabel);
        resolutionField.textProperty().addListener((ignored, oldValue, newValue) -> resolutionErrorLabel.setText(""));
        messageField.textProperty().addListener((ignored, oldValue, newValue) -> messageErrorLabel.setText(""));
        priorityField.valueProperty().addListener((ignored, oldValue, newValue) -> updateBusyState());
    }

    private <T> void addFilterListener(ComboBox<T> filter) {
        filter.valueProperty().addListener((ignored, oldValue, newValue) -> {
            currentPage = 0;
            refreshQueue();
        });
    }

    @Override
    public void onShown() {
        showQueue();
    }

    @FXML
    private void showQueue() {
        showPage(queuePage);
        setQueueNavigationActive();
        refreshQueue();
    }

    @FXML
    private void refreshQueue() {
        if (listLoading || disposed) {
            return;
        }
        listLoading = true;
        queueErrorLabel.setText("");
        updateQueueEmptyCopy();
        updateBusyState();
        run(
                ticketService.list(statusFilter.getValue(), priorityFilter.getValue(),
                        assignmentFilter.getValue(), currentPage),
                page -> {
                    ticketsTable.setItems(FXCollections.observableArrayList(page.content()));
                    ticketCountLabel.setText(
                            page.totalElements() == 1 ? "1 ticket" : page.totalElements() + " tickets");
                    pageCount = page.totalPages();
                    currentPage = page.page();
                    pageLabel.setText(pageCount == 0
                            ? "Page 0 of 0" : "Page " + (currentPage + 1) + " of " + pageCount);
                    listLoading = false;
                    queueStale = false;
                    updateBusyState();
                },
                failure -> {
                    listLoading = false;
                    queueStale = !ticketsTable.getItems().isEmpty();
                    var displayed = showFailure(queueErrorLabel, failure);
                    if (displayed && queueStale) {
                        queueErrorLabel.setText(queueErrorLabel.getText()
                                + " Previously loaded tickets may be out of date. Refresh before opening a ticket.");
                    }
                    updateBusyState();
                });
    }

    @FXML private void previousPage() {
        if (currentPage > 0 && !listLoading) { currentPage--; refreshQueue(); }
    }

    @FXML private void nextPage() {
        if (currentPage + 1 < pageCount && !listLoading) { currentPage++; refreshQueue(); }
    }

    @FXML private void openSelectedTicket() {
        if (listLoading || queueStale) {
            return;
        }
        var ticket = ticketsTable.getSelectionModel().getSelectedItem();
        if (ticket != null) {
            openTicket(ticket.id());
        }
    }

    private void openTicket(int ticketId) {
        if (detailLoading || actionLoading) {
            return;
        }
        detailTicketId = ticketId;
        detailStale = false;
        assigneeField.setValue(null);
        showPage(detailPage);
        setQueueNavigationActive();
        selectedTicket = null;
        detailNumberLabel.setText("Ticket details");
        detailContent.setVisible(false);
        detailContent.setManaged(false);
        loadDetails(ticketId, null);
        if (isManager()) {
            refreshAssignees();
        }
    }

    @FXML private void refreshDetails() {
        if (detailTicketId != null) {
            loadDetails(detailTicketId, null);
        }
    }

    private void loadDetails(int ticketId, String notice) {
        if (detailLoading || actionLoading || disposed) {
            return;
        }
        detailLoading = true;
        detailErrorLabel.setText("");
        detailNoticeLabel.setText("");
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
            if (disposed) {
                return;
            }
            detailLoading = false;
            if (failure != null) {
                detailStale = detailContent.isVisible();
                var displayed = showFailure(detailErrorLabel, failure);
                if (displayed && detailStale) {
                    detailErrorLabel.setText(detailErrorLabel.getText()
                            + " Displayed details may be out of date. Refresh before taking an action.");
                }
                updateBusyState();
                return;
            }
            detailStale = false;
            selectedTicket = data.ticket();
            renderTicket(selectedTicket);
            messagesList.setItems(FXCollections.observableArrayList(data.messages().content()));
            detailContent.setVisible(true);
            detailContent.setManaged(true);
            showDetailNotice(notice, NoticeKind.INFORMATION);
            updateBusyState();
        }));
    }

    @FXML private void takeTicket() {
        if (selectedTicket == null || actionLoading || !confirm("Take ticket",
                "Take " + selectedTicket.ticketNumber() + "?",
                "The ticket will be assigned to you and moved to In progress.",
                "Take ticket", "Cancel", false)) {
            return;
        }
        mutate(ticketService.take(selectedTicket.id()), "Ticket assigned to you.");
    }

    @FXML private void beginWork() {
        if (selectedTicket == null || actionLoading) {
            return;
        }
        mutate(ticketService.beginWork(selectedTicket.id()), "Work started on this ticket.");
    }

    @FXML private void changePriority() {
        if (selectedTicket == null || actionLoading || priorityField.getValue() == null
                || priorityField.getValue() == selectedTicket.priority()) {
            return;
        }
        mutate(ticketService.changePriority(selectedTicket.id(), priorityField.getValue()), "Priority updated.");
    }

    @FXML private void resolveTicket() {
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

    @FXML private void reopenTicket() {
        if (selectedTicket == null || actionLoading || !confirm("Reopen ticket",
                "Reopen " + selectedTicket.ticketNumber() + "?",
                "The ticket will return to the open queue and its assignment and resolution will be cleared.",
                "Reopen ticket", "Cancel", false)) {
            return;
        }
        mutate(ticketService.reopen(selectedTicket.id()), "Ticket reopened and returned to the queue.");
    }

    @FXML private void addMessage() {
        if (selectedTicket == null || actionLoading) {
            return;
        }
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
        run(
                ticketService.addMessage(
                        selectedTicket.id(), messageTypeField.getValue(), messageField.getText()),
                message -> {
                    actionLoading = false;
                    messageField.clear();
                    messagesList.getItems().add(message);
                    messagesList.scrollTo(message);
                    showDetailNotice(message.messageType().equals("INTERNAL_NOTE")
                            ? "Internal note added. It is visible only to IT staff." : "Public comment posted.",
                            NoticeKind.SUCCESS);
                    updateBusyState();
                },
                failure -> {
                    actionLoading = false;
                    updateBusyState();
                    showFailure(messageErrorLabel, failure);
                });
    }

    private void mutate(CompletionStage<Ticket> operation, String notice) {
        actionLoading = true;
        detailErrorLabel.setText("");
        detailNoticeLabel.setText("");
        updateBusyState();
        run(operation, ticket -> {
            actionLoading = false;
            selectedTicket = ticket;
            renderTicket(ticket);
            showDetailNotice(notice, NoticeKind.SUCCESS);
            updateBusyState();
            refreshQueueInBackground();
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
        var currentUserId = session.current().orElseThrow().user().id();
        var assignedToCurrentUser = ticket.assignedToId() != null && ticket.assignedToId() == currentUserId;
        detailNumberLabel.setText(ticket.ticketNumber());
        detailStatusLabel.setText(ticket.status().displayName());
        detailStatusLabel.getStyleClass().removeIf(name -> name.startsWith("status-"));
        detailStatusLabel.getStyleClass().add("status-" + ticket.status().name().toLowerCase().replace('_', '-'));
        detailSubjectLabel.setText(ticket.subject());
        detailDescriptionLabel.setText(ticket.description());
        detailCategoryLabel.setText(ticket.category().displayName());
        detailPriorityLabel.setText(ticket.priority().displayName());
        detailRequesterLabel.setText(ticket.requesterUsername());
        detailAssigneeLabel.setText(
                ticket.assignedToUsername() == null ? "Unassigned" : ticket.assignedToUsername());
        detailCreatedLabel.setText(formatDate(ticket.createdAt()));
        priorityField.setValue(ticket.priority());

        var hasResolution = ticket.resolutionNote() != null && !ticket.resolutionNote().isBlank();
        resolutionBox.setVisible(hasResolution);
        resolutionBox.setManaged(hasResolution);
        resolutionLabel.setText(hasResolution ? ticket.resolutionNote() : "");
        takeButton.setVisible(ticket.status() == TicketStatus.OPEN && ticket.assignedToId() == null);
        takeButton.setManaged(takeButton.isVisible());
        beginWorkButton.setVisible(ticket.status() == TicketStatus.OPEN
                && (assignedToCurrentUser || isManager()) && ticket.assignedToId() != null);
        beginWorkButton.setManaged(beginWorkButton.isVisible());
        reopenButton.setVisible(ticket.status() == TicketStatus.RESOLVED);
        reopenButton.setManaged(reopenButton.isVisible());
        resolveBox.setVisible(
                ticket.status() == TicketStatus.IN_PROGRESS && (assignedToCurrentUser || isManager()));
        resolveBox.setManaged(resolveBox.isVisible());
        cancelButton.setVisible(isManager()
                && (ticket.status() == TicketStatus.OPEN || ticket.status() == TicketStatus.IN_PROGRESS));
        cancelButton.setManaged(cancelButton.isVisible());
        resolutionField.clear();
        resolutionErrorLabel.setText("");
        updateBusyState();
    }

    private void updateBusyState() {
        boolean detailBusy = actionLoading || detailLoading;
        detailContent.setDisable(detailBusy || detailStale);
        queueNavButton.setDisable(detailBusy);
        usersNavButton.setDisable(detailBusy);
        requestsNavButton.setDisable(detailBusy);
        ticketsTable.setDisable(detailBusy || listLoading || queueStale);
        cancelButton.setDisable(detailBusy);
        assigneeField.setDisable(detailBusy || assigneesLoading);
        refreshAssigneesButton.setDisable(detailBusy || assigneesLoading);
        assignButton.setDisable(detailBusy || assigneesLoading || selectedTicket == null
                || assigneeField.getValue() == null || selectedTicket.status() == TicketStatus.RESOLVED
                || selectedTicket.status() == TicketStatus.CANCELLED);
        queueProgress.setVisible(listLoading);
        queueProgress.setManaged(listLoading);
        queueBusyLabel.setText(ticketsTable.getItems().isEmpty() ? "Loading tickets…" : "Refreshing queue…");
        queueBusyLabel.setVisible(listLoading);
        queueBusyLabel.setManaged(listLoading);
        refreshQueueButton.setDisable(listLoading);
        statusFilter.setDisable(listLoading);
        priorityFilter.setDisable(listLoading);
        assignmentFilter.setDisable(listLoading);
        previousButton.setDisable(listLoading || currentPage <= 0);
        nextButton.setDisable(listLoading || currentPage + 1 >= pageCount);
        detailProgress.setVisible(detailLoading || actionLoading || assigneesLoading);
        detailProgress.setManaged(detailProgress.isVisible());
        detailBusyLabel.setText(actionLoading ? "Saving changes…"
                : assigneesLoading ? "Loading assignable users…"
                : selectedTicket == null ? "Loading ticket…" : "Refreshing ticket…");
        detailBusyLabel.setVisible(detailProgress.isVisible());
        detailBusyLabel.setManaged(detailBusyLabel.isVisible());
        refreshDetailButton.setDisable(detailLoading || actionLoading);
        takeButton.setDisable(actionLoading);
        beginWorkButton.setDisable(actionLoading);
        reopenButton.setDisable(actionLoading);
        priorityField.setDisable(actionLoading);
        changePriorityButton.setDisable(actionLoading || selectedTicket == null || priorityField.getValue() == null
                || priorityField.getValue() == selectedTicket.priority());
        resolutionField.setDisable(actionLoading);
        resolveButton.setDisable(actionLoading);
        messageTypeField.setDisable(actionLoading);
        messageField.setDisable(actionLoading);
        addMessageButton.setDisable(actionLoading);
    }

    private void refreshQueueInBackground() {
        if (!listLoading) {
            refreshQueue();
        }
    }

    private void showPage(StackPane page) {
        for (var candidate : new StackPane[] { queuePage, detailPage }) {
            candidate.setVisible(candidate == page);
            candidate.setManaged(candidate == page);
        }
        updateBusyState();
    }

    private TableRow<Ticket> ticketRow() {
        var row = new TableRow<Ticket>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty() && !listLoading && !queueStale) {
                openTicket(row.getItem().id());
            }
        });
        return row;
    }

    private void configureQueueCellDiscovery() {
        numberColumn.setCellFactory(ignored -> tooltipCell());
        subjectColumn.setCellFactory(ignored -> tooltipCell());
        requesterColumn.setCellFactory(ignored -> tooltipCell());
        priorityColumn.setCellFactory(ignored -> tooltipCell());
        statusColumn.setCellFactory(ignored -> tooltipCell());
        assigneeColumn.setCellFactory(ignored -> tooltipCell());
        updatedColumn.setCellFactory(ignored -> tooltipCell());
    }

    private TableCell<Ticket, String> tooltipCell() {
        return new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                tooltip.setText(item);
                setTooltip(empty || item == null || item.isBlank() ? null : tooltip);
            }
        };
    }

    private void resizeQueueColumns(double tableWidth) {
        var extraWidth = Math.max(0, tableWidth - 1_010);
        var ticketWidth = Math.min(190, 125 + extraWidth * 0.45);
        numberColumn.setPrefWidth(ticketWidth);
        subjectColumn.setPrefWidth(260 + Math.max(0, extraWidth - (ticketWidth - 125)));
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

    private <T> void run(CompletionStage<T> operation, Consumer<T> success, Consumer<Throwable> failure) {
        var future = operation.toCompletableFuture();
        track(future);
        future.whenComplete((result, problem) -> Platform.runLater(() -> {
            inFlight.remove(future);
            if (disposed) {
                return;
            }
            if (problem == null) {
                success.accept(result);
            } else {
                failure.accept(problem);
            }
        }));
    }

    private void track(CompletableFuture<?> future) { inFlight.add(future); }

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
        return unwrap(problem) instanceof TicketFailure failure && failure.kind() == TicketFailure.Kind.CONFLICT;
    }

    private String conflictNotice() {
        return "This ticket changed elsewhere. We reloaded the latest details.";
    }

    private void setQueueNavigationActive() {
        if (!queueNavButton.getStyleClass().contains("nav-button-active")) {
            queueNavButton.getStyleClass().add("nav-button-active");
        }
    }

    private void showDetailNotice(String message, NoticeKind kind) {
        detailNoticeBox.getStyleClass().removeAll("success-message", "information-message");
        detailNoticeBox.getStyleClass().add(kind == NoticeKind.SUCCESS
                ? "success-message" : "information-message");
        detailNoticeMark.setVisible(kind == NoticeKind.SUCCESS);
        detailNoticeMark.setManaged(kind == NoticeKind.SUCCESS);
        detailNoticeLabel.setText(message == null ? "" : message);
    }

    private void updateQueueEmptyCopy() {
        var defaultFilters = statusFilter.getValue() == null
                && priorityFilter.getValue() == null
                && assignmentFilter.getValue() == AssignmentFilter.ALL;
        queueEmptyLabel.setText(defaultFilters
                ? "No tickets are currently visible." : "No tickets match these filters.");
    }

    private void updateMessageComposerCopy(MessageType messageType) {
        var internal = messageType == MessageType.INTERNAL_NOTE;
        messageField.setPromptText(internal
                ? "Write a note visible only to IT staff…"
                : "Write an update the requester can see…");
        addMessageButton.setText(internal ? "_Add internal note" : "_Post public comment");
    }

    private Throwable unwrap(Throwable problem) {
        var current = problem;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private void bindManaged(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    private void bindManaged(HBox container, Label label) {
        container.visibleProperty().bind(label.textProperty().isNotEmpty());
        container.managedProperty().bind(container.visibleProperty());
    }

    private SimpleStringProperty text(String value) { return new SimpleStringProperty(value == null ? "" : value); }

    private String formatDate(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        try {
            return DATE_FORMAT.format(Instant.parse(value).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException ignored) {
            return value;
        }
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

    private <T> StringConverter<T> nullableConverter(String nullText,
                                                       java.util.function.Function<T, String> displayName) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? nullText : displayName.apply(value); }
            @Override public T fromString(String value) { throw new UnsupportedOperationException(); }
        };
    }

    @FXML private void logout() {
        if (logoutButton.isDisabled()) {
            return;
        }
        logoutButton.setDisable(true);
        navigator.signOut();
    }

    @Override
    public void dispose() {
        disposed = true;
        for (var future : Set.copyOf(inFlight)) {
            future.cancel(true);
        }
        inFlight.clear();
    }

    private record DetailData(Ticket ticket, PageResponse<TicketMessage> messages) {}

    private enum NoticeKind {
        SUCCESS, INFORMATION
    }

    private final class MessageCell extends ListCell<TicketMessage> {
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
