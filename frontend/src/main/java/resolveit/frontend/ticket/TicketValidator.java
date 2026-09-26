package resolveit.frontend.ticket;

/**
 * Provides client-side feedback for ticket and message input before submission.
 * The backend remains authoritative for validation and authorization.
 */
public final class TicketValidator {
    private TicketValidator() {}

    /**
     * Validates and trims the fields required to submit a ticket.
     *
     * @param subject ticket subject
     * @param description ticket description
     * @param category ticket category
     * @param priority ticket priority
     * @return validation errors, or a valid result when every field passes
     */
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

    /**
     * Validates a message after treating null and surrounding whitespace as empty input.
     *
     * @param message comment or internal-note body
     * @return an error message when invalid, or null when valid
     */
    public static String validateMessage(String message) {
        var normalized = message == null ? "" : message.trim();
        return normalized.isEmpty() || normalized.length() > 5_000
                ? "Comment must contain 1 to 5,000 characters." : null;
    }

    /**
     * Holds the client-side error messages for each ticket field.
     *
     * @param subjectError subject error, or null when valid
     * @param descriptionError description error, or null when valid
     * @param categoryError category error, or null when valid
     * @param priorityError priority error, or null when valid
     */
    public record Validation(String subjectError, String descriptionError,
                             String categoryError, String priorityError) {
        /**
         * Reports whether every ticket field passed validation.
         *
         * @return true when all error fields are null
         */
        public boolean isValid() {
            return subjectError == null && descriptionError == null && categoryError == null && priorityError == null;
        }
    }
}
