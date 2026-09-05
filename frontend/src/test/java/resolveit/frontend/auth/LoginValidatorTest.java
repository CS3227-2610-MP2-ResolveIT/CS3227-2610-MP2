package resolveit.frontend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginValidatorTest {
    @Test
    void acceptsValidCredentials() {
        var result = LoginValidator.validate("employee@resolveit.local", "Employee123!");

        assertTrue(result.isValid());
        assertNull(result.emailError());
        assertNull(result.passwordError());
    }

    @Test
    void reportsBothBlankFields() {
        var result = LoginValidator.validate("  ", "");

        assertFalse(result.isValid());
        assertEquals("Enter your email address.", result.emailError());
        assertEquals("Enter your password.", result.passwordError());
    }

    @Test
    void rejectsMalformedEmail() {
        var result = LoginValidator.validate("not-an-email", "secret");

        assertFalse(result.isValid());
        assertEquals("Enter a valid email address.", result.emailError());
    }
}
