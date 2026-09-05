package resolveit.frontend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicketValidatorTest {
    @Test
    void acceptsValidTrimmedTicketFields() {
        var result = TicketValidator.validateTicket("  Wi-Fi unavailable  ",
                "  I cannot connect from my laptop.  ", TicketCategory.NETWORK, TicketPriority.HIGH);

        assertTrue(result.isValid());
    }

    @Test
    void reportsEveryInvalidTicketField() {
        var result = TicketValidator.validateTicket("bad", "short", null, null);

        assertFalse(result.isValid());
        assertEquals("Subject must contain 5 to 200 characters.", result.subjectError());
        assertEquals("Description must contain 10 to 10,000 characters.", result.descriptionError());
        assertEquals("Choose a category.", result.categoryError());
        assertEquals("Choose a priority.", result.priorityError());
    }

    @Test
    void validatesPublicCommentLength() {
        assertEquals("Comment must contain 1 to 5,000 characters.", TicketValidator.validateMessage("   "));
        assertNull(TicketValidator.validateMessage("Still happening after a restart."));
    }
}
