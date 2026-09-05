package resolveit.frontend.auth;

import java.util.regex.Pattern;

public final class LoginValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private LoginValidator() {
    }

    public static LoginValidation validate(String email, String password) {
        String emailError = null;
        String passwordError = null;

        if (email == null || email.isBlank()) {
            emailError = "Enter your email address.";
        } else if (email.length() > 254 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            emailError = "Enter a valid email address.";
        }
        if (password == null || password.isBlank()) {
            passwordError = "Enter your password.";
        }

        return new LoginValidation(emailError, passwordError);
    }
}
