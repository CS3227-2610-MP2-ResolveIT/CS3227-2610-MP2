package resolveit.frontend.ticket;

public final class TicketValidator {
    private TicketValidator() {}

    public static Validation validateTicket(String subject, String description,
                                            TicketCategory category, TicketPriority priority) {
        var normalizedSubject = subject == null ? "" : subject.trim();
        var normalizedDescription = description == null ? "" : description.trim();
        String subjectError = normalizedSubject.length() < 5 || normalizedSubject.length() > 200
                ? "Subject must contain 5 to 200 characters." : null;
        String descriptionError = normalizedDescription.length() < 10 || normalizedDescription.length() > 10_000
                ? "Description must contain 10 to 10,000 characters." : null;
        String categoryError = category == null ? "Choose a category." : null;
        String priorityError = priority == null ? "Choose a priority." : null;
        return new Validation(subjectError, descriptionError, categoryError, priorityError);
    }

    public static String validateMessage(String message) {
        var normalized = message == null ? "" : message.trim();
        return normalized.isEmpty() || normalized.length() > 5_000
                ? "Comment must contain 1 to 5,000 characters." : null;
    }

    public record Validation(String subjectError, String descriptionError,
                             String categoryError, String priorityError) {
        public boolean isValid() {
            return subjectError == null && descriptionError == null && categoryError == null && priorityError == null;
        }
    }
}
