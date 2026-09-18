package resolveit.frontend.ui;

import java.util.function.Function;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import resolveit.frontend.ticket.Ticket;
import resolveit.frontend.ticket.TicketPriority;
import resolveit.frontend.ticket.TicketStatus;

/** Renders Technician ticket-detail data and the actions currently available for that ticket. */
final class TechnicianTicketDetailRenderer {
    void render(Ticket ticket, int currentUserId, boolean manager, Function<String, String> formatDate,
                DetailView view) {
        var assignedToCurrentUser = ticket.assignedToId() != null && ticket.assignedToId() == currentUserId;
        view.numberLabel().setText(ticket.ticketNumber());
        view.statusLabel().setText(ticket.status().displayName());
        view.statusLabel().getStyleClass().removeIf(name -> name.startsWith("status-"));
        view.statusLabel().getStyleClass().add("status-" + ticket.status().name().toLowerCase().replace('_', '-'));
        view.subjectLabel().setText(ticket.subject());
        view.descriptionLabel().setText(ticket.description());
        view.categoryLabel().setText(ticket.category().displayName());
        view.priorityLabel().setText(ticket.priority().displayName());
        view.requesterLabel().setText(ticket.requesterUsername());
        view.assigneeLabel().setText(ticket.assignedToUsername() == null ? "Unassigned" : ticket.assignedToUsername());
        view.createdLabel().setText(formatDate.apply(ticket.createdAt()));
        view.priorityField().setValue(ticket.priority());

        var hasResolution = ticket.resolutionNote() != null && !ticket.resolutionNote().isBlank();
        view.resolutionBox().setVisible(hasResolution);
        view.resolutionBox().setManaged(hasResolution);
        view.resolutionLabel().setText(hasResolution ? ticket.resolutionNote() : "");
        updateVisible(view.takeButton(), ticket.status() == TicketStatus.OPEN && ticket.assignedToId() == null);
        updateVisible(view.beginWorkButton(), ticket.status() == TicketStatus.OPEN
                && (assignedToCurrentUser || manager) && ticket.assignedToId() != null);
        updateVisible(view.reopenButton(), ticket.status() == TicketStatus.RESOLVED);
        updateVisible(view.resolveBox(), ticket.status() == TicketStatus.IN_PROGRESS
                && (assignedToCurrentUser || manager));
        updateVisible(view.cancelButton(), manager
                && (ticket.status() == TicketStatus.OPEN || ticket.status() == TicketStatus.IN_PROGRESS));
        view.resolutionField().clear();
        view.resolutionErrorLabel().setText("");
    }

    private static void updateVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    record DetailView(Label numberLabel, Label statusLabel, Label subjectLabel, Label descriptionLabel,
                      Label categoryLabel, Label priorityLabel, Label requesterLabel, Label assigneeLabel,
                      Label createdLabel, ComboBox<TicketPriority> priorityField, VBox resolutionBox,
                      Label resolutionLabel, Button takeButton, Button beginWorkButton, Button reopenButton,
                      VBox resolveBox, Button cancelButton, TextArea resolutionField, Label resolutionErrorLabel) {}
}
