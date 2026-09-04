package resolveit.frontend.auth;

import java.util.concurrent.CompletionStage;
import resolveit.frontend.session.SessionState;

public final class AuthService {
    private final AuthClient authClient;
    private final SessionState session;

    public AuthService(AuthClient authClient, SessionState session) {
        this.authClient = authClient;
        this.session = session;
    }

    public CompletionStage<LoginResponse> login(String email, String password) {
        var request = new LoginRequest(email.trim(), password);
        return authClient.login(request).thenApply(response -> {
            session.start(response);
            return response;
        });
    }
}
