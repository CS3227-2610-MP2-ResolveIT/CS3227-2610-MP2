package resolveit.frontend.auth;

public record LoginValidation(String emailError, String passwordError) {
    public boolean isValid() {
        return emailError == null && passwordError == null;
    }
}
