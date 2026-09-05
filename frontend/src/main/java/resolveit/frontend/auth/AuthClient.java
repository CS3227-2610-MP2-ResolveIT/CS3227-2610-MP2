package resolveit.frontend.auth;

import java.util.concurrent.CompletionStage;

public interface AuthClient {
    CompletionStage<LoginResponse> login(LoginRequest request);
}
