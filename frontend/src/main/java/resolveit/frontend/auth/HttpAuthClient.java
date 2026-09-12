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

/** Sends authentication requests using Java's asynchronous HTTP facilities. */
public final class HttpAuthClient implements AuthClient, AutoCloseable {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final URI loginUrl;
    private final URI refreshUrl;
    private final URI logoutUrl;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    /**
     * Creates a client using the default connection and request timeouts.
     *
     * @param apiBaseUrl API base URL ending before the authentication paths
     * @return authentication HTTP client
     */
    public static HttpAuthClient create(URI apiBaseUrl) {
        return new HttpAuthClient(apiBaseUrl, DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    HttpAuthClient(URI apiBaseUrl, Duration connectTimeout, Duration requestTimeout) {
        loginUrl = apiBaseUrl.resolve("auth/login");
        refreshUrl = apiBaseUrl.resolve("auth/refresh");
        logoutUrl = apiBaseUrl.resolve("auth/logout");
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

    @Override
    public CompletionStage<RefreshResponse> refresh(RefreshRequest refreshRequest) {
        return CompletableFuture.supplyAsync(() -> sendRefresh(refreshRequest), executor);
    }

    @Override
    public CompletionStage<Void> logout(LogoutRequest logoutRequest, String authorization) {
        return CompletableFuture.runAsync(() -> sendLogout(logoutRequest, authorization), executor);
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

    private RefreshResponse sendRefresh(RefreshRequest refreshRequest) {
        try {
            var body = objectMapper.writeValueAsString(refreshRequest);
            var request = jsonRequest(refreshUrl, body).build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleRefreshResponse(response);
        } catch (HttpTimeoutException exception) {
            throw timeout("The session refresh timed out. Please sign in again.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw connection("The session refresh was interrupted. Please sign in again.", exception);
        } catch (IOException exception) {
            throw connection("Unable to reach ResolveIT while refreshing the session.", exception);
        }
    }

    private void sendLogout(LogoutRequest logoutRequest, String authorization) {
        try {
            var body = objectMapper.writeValueAsString(logoutRequest);
            var request = jsonRequest(logoutUrl, body)
                    .header("Authorization", authorization)
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw response.statusCode() == 401
                        ? sessionExpired()
                        : new AuthFailure(AuthFailure.Kind.SERVER,
                                "ResolveIT could not complete server-side sign-out.");
            }
        } catch (HttpTimeoutException exception) {
            throw timeout("Server-side sign-out timed out.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw connection("Server-side sign-out was interrupted.", exception);
        } catch (IOException exception) {
            throw connection("Unable to reach ResolveIT during sign-out.", exception);
        }
    }

    private HttpRequest.Builder jsonRequest(URI uri, String body) {
        return HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private LoginResponse handleResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            try {
                var login = objectMapper.readValue(response.body(), LoginResponse.class);
                if (login.accessToken() == null || login.accessToken().isBlank()
                        || login.refreshToken() == null || login.refreshToken().isBlank()
                        || login.tokenType() == null || login.tokenType().isBlank()
                        || login.expiresIn() <= 0
                        || login.refreshExpiresIn() <= 0
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
        if (status == 429) {
            throw new AuthFailure(AuthFailure.Kind.RATE_LIMITED,
                    "Too many sign-in attempts. Please wait 1 minute before trying again.");
        }
        if (status >= 500) {
            throw new AuthFailure(AuthFailure.Kind.SERVER,
                    "ResolveIT is temporarily unavailable. Please try again.");
        }
        throw new AuthFailure(AuthFailure.Kind.SERVER,
                "Sign-in could not be completed. Please try again.");
    }

    private RefreshResponse handleRefreshResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            try {
                var refresh = objectMapper.readValue(response.body(), RefreshResponse.class);
                if (refresh.accessToken() == null || refresh.accessToken().isBlank()
                        || refresh.refreshToken() == null || refresh.refreshToken().isBlank()
                        || refresh.tokenType() == null || refresh.tokenType().isBlank()
                        || refresh.expiresIn() <= 0 || refresh.refreshExpiresIn() <= 0) {
                    throw invalidResponse(null);
                }
                return refresh;
            } catch (AuthFailure failure) {
                throw failure;
            } catch (RuntimeException exception) {
                throw invalidResponse(exception);
            }
        }
        if (status == 400 || status == 401) {
            throw sessionExpired();
        }
        if (status >= 500) {
            throw new AuthFailure(AuthFailure.Kind.SERVER,
                    "ResolveIT is temporarily unavailable. Please sign in again.");
        }
        throw sessionExpired();
    }

    private AuthFailure timeout(String message, Throwable cause) {
        return new AuthFailure(AuthFailure.Kind.TIMEOUT, message, cause);
    }

    private AuthFailure connection(String message, Throwable cause) {
        return new AuthFailure(AuthFailure.Kind.CONNECTION, message, cause);
    }

    private AuthFailure sessionExpired() {
        return new AuthFailure(AuthFailure.Kind.SESSION_EXPIRED,
                "Your session has expired. Please sign in again.");
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
