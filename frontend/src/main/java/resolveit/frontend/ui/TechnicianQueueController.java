package resolveit.frontend.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import resolveit.frontend.ticket.TechnicianTicketService;
import resolveit.frontend.ticket.TechnicianTicketService.AssignmentFilter;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketFailure;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketStatus;

/** Owns ticket-queue filters, pagination, loading state, and ticket selection. */
public final class TechnicianQueueController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a");

    private final TechnicianTicketService ticketService;
    private final boolean manager;
    private final AsyncOperationTracker operations = new AsyncOperationTracker();
    private Consumer<Integer> ticketSelected = ignored -> { };
    private Consumer<Throwable> failureHandler = ignored -> { };
    private boolean disposed;
    private boolean loading;
    private boolean stale;
    private boolean detailBusy;
    private int currentPage;
    private int pageCount;

    @FXML private Label queueHeading;
    @FXML private Label queueDescription;
    @FXML private Label statusFilterLabel;
    @FXML private Label priorityFilterLabel;
    @FXML private Label assignmentFilterLabel;
    @FXML private Label queueBusyLabel;
    @FXML private Label queueErrorLabel;
    @FXML private Label queueEmptyLabel;
    @FXML private Label ticketCountLabel;
    @FXML private Label pageLabel;
    @FXML private ComboBox<TicketStatus> statusFilter;
    @FXML private ComboBox<TicketPriority> priorityFilter;
    @FXML private ComboBox<AssignmentFilter> assignmentFilter;
    @FXML private Button refreshQueueButton;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private ProgressIndicator queueProgress;
    @FXML private TableView<Ticket> ticketsTable;
    @FXML private TableColumn<Ticket, String> numberColumn;
    @FXML private TableColumn<Ticket, String> subjectColumn;
    @FXML private TableColumn<Ticket, String> requesterColumn;
    @FXML private TableColumn<Ticket, String> priorityColumn;
    @FXML private TableColumn<Ticket, String> statusColumn;
    @FXML private TableColumn<Ticket, String> assigneeColumn;
    @FXML private TableColumn<Ticket, String> updatedColumn;

    /**
     * Creates the queue child controller for a technician or manager workspace.
     *
     * @param ticketService service used to retrieve the visible ticket queue
     * @param manager whether the current user is a manager who can view cancelled tickets
     */
    public TechnicianQueueController(TechnicianTicketService ticketService, boolean manager) {
        this.ticketService = ticketService;
        this.manager = manager;
    }

    @FXML
    private void initialize() {
        statusFilterLabel.setLabelFor(statusFilter);
        priorityFilterLabel.setLabelFor(priorityFilter);
        assignmentFilterLabel.setLabelFor(assignmentFilter);
        statusFilter.setItems(FXCollections.observableArrayList(
                TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED));
        statusFilter.getItems().addFirst(null);
        if (manager) {
            statusFilter.getItems().add(TicketStatus.CANCELLED);
            queueHeading.setText("All Tickets");
            queueDescription.setText(
                    "Review all requests, including resolved and cancelled tickets, and manage assignments.");
            ticketsTable.setAccessibleText("All tickets");
        }
        priorityFilter.setItems(FXCollections.observableArrayList(TicketPriority.values()));
        priorityFilter.getItems().addFirst(null);
        assignmentFilter.setItems(FXCollections.observableArrayList(AssignmentFilter.values()));
        statusFilter.setConverter(Converters.nullable("All statuses", TicketStatus::displayName));
        priorityFilter.setConverter(Converters.nullable("All priorities", TicketPriority::displayName));
        assignmentFilter.setConverter(Converters.nullable(
                "Any assignment",
                assignment -> assignment == AssignmentFilter.ALL
                        ? "Any assignment" : assignment.displayName()));
        assignmentFilter.setValue(AssignmentFilter.ALL);
        addFilterListener(statusFilter);
        addFilterListener(priorityFilter);
        addFilterListener(assignmentFilter);
        configureColumns();
        configureInteraction();
        bindManaged(queueErrorLabel);
    }

    void setTicketSelectedHandler(Consumer<Integer> handler) {
        ticketSelected = handler;
    }

    void setFailureHandler(Consumer<Throwable> handler) {
        failureHandler = handler;
    }

    void showQueue() {
        refreshQueue();
    }

    void refreshInBackground() {
        if (!loading) {
            refreshQueue();
        }
    }

    void setDetailBusy(boolean busy) {
        detailBusy = busy;
        updateBusyState();
    }

    void dispose() {
        disposed = true;
        operations.cancelAll();
    }

    @FXML
    /** Loads the selected queue page and ignores late results after disposal. */
    private void refreshQueue() {
        if (loading || disposed) {
            return;
        }
        loading = true;
        queueErrorLabel.setText("");
        updateEmptyCopy();
        updateBusyState();
        operations.run(
                ticketService.list(statusFilter.getValue(), priorityFilter.getValue(),
                        assignmentFilter.getValue(), currentPage),
                () -> disposed,
                page -> {
                    ticketsTable.setItems(FXCollections.observableArrayList(page.content()));
                    ticketCountLabel.setText(page.totalElements() == 1
                            ? "1 ticket" : page.totalElements() + " tickets");
                    pageCount = page.totalPages();
                    currentPage = page.page();
                    pageLabel.setText(pageCount == 0
                            ? "Page 0 of 0" : "Page " + (currentPage + 1) + " of " + pageCount);
                    loading = false;
                    stale = false;
                    updateBusyState();
                },
                failure -> {
                    loading = false;
                    stale = !ticketsTable.getItems().isEmpty();
                    if (failure instanceof TicketFailure ticketFailure) {
                        queueErrorLabel.setText(ticketFailure.getMessage());
                    } else {
                        queueErrorLabel.setText("The request could not be completed. Please try again.");
                    }
                    if (stale) {
                        queueErrorLabel.setText(queueErrorLabel.getText()
                                + " Previously loaded tickets may be out of date."
                                + " Refresh before opening a ticket.");
                    }
                    failureHandler.accept(failure);
                    updateBusyState();
                });
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0 && !loading) {
            currentPage--;
            refreshQueue();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage + 1 < pageCount && !loading) {
            currentPage++;
            refreshQueue();
        }
    }

    @FXML
    private void openSelectedTicket() {
        if (loading || stale) {
            return;
        }
        var ticket = ticketsTable.getSelectionModel().getSelectedItem();
        if (ticket != null) {
            ticketSelected.accept(ticket.id());
        }
    }

    private <T> void addFilterListener(ComboBox<T> filter) {
        filter.valueProperty().addListener((ignored, oldValue, newValue) -> {
            currentPage = 0;
            refreshQueue();
        });
    }

    private void configureColumns() {
        numberColumn.setCellValueFactory(cell -> text(cell.getValue().ticketNumber()));
        subjectColumn.setCellValueFactory(cell -> text(cell.getValue().subject()));
        requesterColumn.setCellValueFactory(cell -> text(cell.getValue().requesterUsername()));
        priorityColumn.setCellValueFactory(cell -> text(cell.getValue().priority().displayName()));
        statusColumn.setCellValueFactory(cell -> text(cell.getValue().status().displayName()));
        assigneeColumn.setCellValueFactory(cell -> text(cell.getValue().assignedToUsername() == null
                ? "Unassigned" : cell.getValue().assignedToUsername()));
        updatedColumn.setCellValueFactory(cell -> text(formatDate(cell.getValue().updatedAt())));
        configureTooltipCells();
    }

    private void configureTooltipCells() {
        numberColumn.setCellFactory(ignored -> tooltipCell());
        subjectColumn.setCellFactory(ignored -> tooltipCell());
        requesterColumn.setCellFactory(ignored -> tooltipCell());
        priorityColumn.setCellFactory(ignored -> tooltipCell());
        statusColumn.setCellFactory(ignored -> tooltipCell());
        assigneeColumn.setCellFactory(ignored -> tooltipCell());
        updatedColumn.setCellFactory(ignored -> tooltipCell());
    }

    /** Connects queue controls to navigation and refresh behavior. */
    private void configureInteraction() {
        ticketsTable.widthProperty().addListener((ignored, oldWidth, newWidth) ->
                resizeColumns(newWidth.doubleValue()));
        ticketsTable.setRowFactory(ignored -> ticketRow());
        ticketsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                openSelectedTicket();
            }
        });
    }

    /** Disables queue actions while a request is in flight. */
    private void updateBusyState() {
        ticketsTable.setDisable(detailBusy || loading || stale);
        queueProgress.setVisible(loading);
        queueProgress.setManaged(loading);
        queueBusyLabel.setText(ticketsTable.getItems().isEmpty()
                ? "Loading tickets…" : "Refreshing queue…");
        queueBusyLabel.setVisible(loading);
        queueBusyLabel.setManaged(loading);
        refreshQueueButton.setDisable(loading);
        statusFilter.setDisable(loading);
        priorityFilter.setDisable(loading);
        assignmentFilter.setDisable(loading);
        previousButton.setDisable(loading || currentPage <= 0);
        nextButton.setDisable(loading || currentPage + 1 >= pageCount);
    }

    private TableRow<Ticket> ticketRow() {
        var row = new TableRow<Ticket>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty() && !loading && !stale) {
                ticketSelected.accept(row.getItem().id());
            }
        });
        return row;
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

    private void resizeColumns(double width) {
        var extra = Math.max(0, width - 1_010);
        var ticketWidth = Math.min(190, 125 + extra * .45);
        numberColumn.setPrefWidth(ticketWidth);
        subjectColumn.setPrefWidth(260 + Math.max(0, extra - (ticketWidth - 125)));
    }

    private void updateEmptyCopy() {
        var defaults = statusFilter.getValue() == null
                && priorityFilter.getValue() == null
                && assignmentFilter.getValue() == AssignmentFilter.ALL;
        queueEmptyLabel.setText(defaults
                ? "No tickets are currently visible." : "No tickets match these filters.");
    }

    private void bindManaged(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }

    private static SimpleStringProperty text(String value) {
        return new SimpleStringProperty(value == null ? "" : value);
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
}
