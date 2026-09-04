package resolveit.frontend.auth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tools.jackson.databind.ObjectMapper;

public final class HttpAuthClient implements AuthClient, AutoCloseable {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final URI loginUrl;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    public static HttpAuthClient create(URI apiBaseUrl) {
        return new HttpAuthClient(apiBaseUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    HttpAuthClient(URI apiBaseUrl, Duration connectTimeout, Duration requestTimeout) {
        loginUrl = apiBaseUrl.resolve("auth/login");
        this.requestTimeout = requestTimeout;
        objectMapper = new ObjectMapper();
        executor = Executors.newVirtualThreadPerTaskExecutor();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .executor(executor)
                .build();
    }

    @Override
    public CompletionStage<LoginResponse> login(LoginRequest loginRequest) {
        return CompletableFuture.supplyAsync(() -> sendLogin(loginRequest), executor);
    }

    private LoginResponse sendLogin(LoginRequest loginRequest) {
        try {
            var body = objectMapper.writeValueAsString(loginRequest);
            var request = HttpRequest.newBuilder(loginUrl)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (HttpTimeoutException exception) {
            throw new AuthFailure(AuthFailure.Kind.TIMEOUT,
                    "The server took too long to respond. Please try again.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthFailure(AuthFailure.Kind.CONNECTION,
                    "The sign-in request was interrupted. Please try again.", exception);
        } catch (IOException exception) {
            throw new AuthFailure(AuthFailure.Kind.CONNECTION,
                    "Unable to reach ResolveIT. Check that the backend is running.", exception);
        }
    }

    private LoginResponse handleResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            try {
                var login = objectMapper.readValue(response.body(), LoginResponse.class);
                if (login.accessToken() == null || login.accessToken().isBlank()
                        || login.tokenType() == null || login.tokenType().isBlank()
                        || login.expiresIn() <= 0
                        || login.user() == null || login.user().role() == null) {
                    throw invalidResponse(null);
                }
                return login;
            } catch (AuthFailure failure) {
                throw failure;
            } catch (RuntimeException exception) {
                throw invalidResponse(exception);
            }
        }

        var apiError = readApiError(response.body());
        if (status == 401) {
            throw new AuthFailure(AuthFailure.Kind.INVALID_CREDENTIALS,
                    "The email or password is incorrect.");
        }
        if (status == 400) {
            throw new AuthFailure(AuthFailure.Kind.INVALID_REQUEST,
                    apiError == null ? "The sign-in details are invalid." : apiError.message());
        }
        if (status >= 500) {
            throw new AuthFailure(AuthFailure.Kind.SERVER,
                    "ResolveIT is temporarily unavailable. Please try again.");
        }
        throw new AuthFailure(AuthFailure.Kind.SERVER,
                "Sign-in could not be completed. Please try again.");
    }

    private ApiError readApiError(String body) {
        try {
            return objectMapper.readValue(body, ApiError.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private AuthFailure invalidResponse(Throwable cause) {
        return new AuthFailure(AuthFailure.Kind.INVALID_RESPONSE,
                "ResolveIT returned an unexpected response. Please try again.", cause);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
