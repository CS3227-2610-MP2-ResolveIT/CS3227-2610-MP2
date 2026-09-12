package resolveit.frontend.ticket;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import resolveit.frontend.auth.AuthFailure;
import resolveit.frontend.auth.AuthenticatedSession;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.session.SessionState.Session;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Sends authenticated ticket and manager requests with one renewal retry. */
public final class HttpTicketClient implements TicketClient, resolveit.frontend.user.ManagerClient, AutoCloseable {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final URI ticketsUrl;
    private final AuthenticatedSession authenticatedSession;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClient httpClient;

    /**
     * Creates a client that obtains valid access credentials from the session coordinator.
     *
     * @param apiBaseUrl API base URL ending before the ticket path
     * @param authenticatedSession coordinator for current and refreshed credentials
     * @return authenticated ticket HTTP client
     */
    public static HttpTicketClient create(URI apiBaseUrl, AuthenticatedSession authenticatedSession) {
        return new HttpTicketClient(apiBaseUrl, authenticatedSession, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
    }

    HttpTicketClient(URI apiBaseUrl, SessionState session, Duration connectTimeout, Duration requestTimeout) {
        this(apiBaseUrl, fixedSession(session), connectTimeout, requestTimeout);
    }

    HttpTicketClient(URI apiBaseUrl, AuthenticatedSession authenticatedSession,
                     Duration connectTimeout, Duration requestTimeout) {
        this.ticketsUrl = apiBaseUrl.resolve("tickets");
        this.authenticatedSession = authenticatedSession;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).executor(executor).build();
    }

    @Override
    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size) {
        return list(status, null, null, null, page, size);
    }

    @Override
    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, TicketPriority priority,
                                                       Boolean assignedToMe, Boolean unassigned,
                                                       int page, int size) {
        var suffix = "?page=" + page + "&size=" + size
                + queryParameter("status", status == null ? null : status.name())
                + queryParameter("priority", priority == null ? null : priority.name())
                + queryParameter("assignedToMe", assignedToMe)
                + queryParameter("unassigned", unassigned);
        return send("GET", URI.create(ticketsUrl + suffix), null, new TypeReference<PageResponse<Ticket>>() {});
    }

    @Override
    public CompletionStage<Ticket> get(int ticketId) {
        return send("GET", ticketUrl(ticketId), null, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> create(CreateTicket request) {
        return send("POST", ticketsUrl, request, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> update(int ticketId, UpdateTicket request) {
        return send("PATCH", ticketUrl(ticketId), request, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<PageResponse<TicketMessage>> messages(int ticketId) {
        return send("GET", URI.create(ticketUrl(ticketId) + "/messages?page=0&size=100"), null,
                new TypeReference<PageResponse<TicketMessage>>() {});
    }

    @Override
    public CompletionStage<TicketMessage> addComment(int ticketId, CreateMessage request) {
        return send("POST", URI.create(ticketUrl(ticketId) + "/messages"), request,
                new TypeReference<TicketMessage>() {});
    }

    @Override
    public CompletionStage<Ticket> cancel(int ticketId) {
        return send("POST", URI.create(ticketUrl(ticketId) + "/cancel"), null, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> reopen(int ticketId) {
        return send("POST", URI.create(ticketUrl(ticketId) + "/reopen"), null, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> take(int ticketId) {
        return send("POST", URI.create(ticketUrl(ticketId) + "/take"), null, new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> changeStatus(int ticketId, ChangeStatus request) {
        return send("PATCH", URI.create(ticketUrl(ticketId) + "/status"), request,
                new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> changePriority(int ticketId, ChangePriority request) {
        return send("PATCH", URI.create(ticketUrl(ticketId) + "/priority"), request,
                new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<Ticket> resolve(int ticketId, ResolveTicket request) {
        return send("POST", URI.create(ticketUrl(ticketId) + "/resolve"), request,
                new TypeReference<Ticket>() {});
    }

    @Override
    public CompletionStage<PageResponse<resolveit.frontend.model.User>> users(int page) {
        return send("GET", ticketsUrl.resolve("users?page=" + page + "&size=20"), null, new TypeReference<>() {});
    }

    @Override
    public CompletionStage<java.util.List<resolveit.frontend.model.User>> technicians() {
        return send("GET", ticketsUrl.resolve("technicians"), null, new TypeReference<>() {});
    }

    @Override
    public CompletionStage<resolveit.frontend.model.User> createUser(UserRequest request) {
        return send("POST", ticketsUrl.resolve("users"), request, new TypeReference<>() {});
    }

    @Override
    public CompletionStage<resolveit.frontend.model.User> updateUser(int id, UserRequest request) {
        return send("PATCH", ticketsUrl.resolve("users/" + id), request, new TypeReference<>() {});
    }

    @Override
    public CompletionStage<Ticket> assign(int id, int technicianId) {
        return send("POST", URI.create(ticketUrl(id) + "/assign"),
                java.util.Map.of("technicianId", technicianId), new TypeReference<>() {});
    }

    private <T> CompletionStage<T> send(String method, URI uri, Object body, TypeReference<T> type) {
        return CompletableFuture.supplyAsync(() -> sendWithRefresh(method, uri, body, type), executor);
    }

    private <T> T sendWithRefresh(String method, URI uri, Object body, TypeReference<T> type) {
        var current = await(authenticatedSession.validSession());
        try {
            return sendBlocking(method, uri, body, type, current);
        } catch (TicketFailure failure) {
            if (failure.kind() != TicketFailure.Kind.UNAUTHORIZED) {
                throw failure;
            }
            var refreshed = await(authenticatedSession.refreshSession());
            return sendBlocking(method, uri, body, type, refreshed);
        }
    }

    private <T> T sendBlocking(String method, URI uri, Object body, TypeReference<T> type, Session current) {
        try {
            var publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
            var request = HttpRequest.newBuilder(uri)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", current.tokenType() + " " + current.accessToken())
                    .method(method, publisher)
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try {
                    return objectMapper.readValue(response.body(), type);
                } catch (RuntimeException exception) {
                    throw new TicketFailure(TicketFailure.Kind.INVALID_RESPONSE, "INVALID_RESPONSE",
                            "ResolveIT returned an unexpected response. Please refresh and try again.", exception);
                }
            }
            throw mapFailure(response.statusCode(), response.body());
        } catch (HttpTimeoutException exception) {
            throw new TicketFailure(TicketFailure.Kind.TIMEOUT, "TIMEOUT",
                    "The server took too long to respond. Please try again.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TicketFailure(TicketFailure.Kind.CONNECTION, "INTERRUPTED",
                    "The request was interrupted. Please try again.", exception);
        } catch (IOException exception) {
            throw new TicketFailure(TicketFailure.Kind.CONNECTION, "CONNECTION_FAILED",
                    "Unable to reach ResolveIT. Check that the backend is running.", exception);
        }
    }

    private Session await(CompletionStage<Session> operation) {
        try {
            return operation.toCompletableFuture().join();
        } catch (CompletionException exception) {
            var cause = exception.getCause();
            if (cause instanceof TicketFailure failure) {
                throw failure;
            }
            var message = cause instanceof AuthFailure failure
                    ? failure.getMessage()
                    : "Your session has ended. Please sign in again.";
            throw new TicketFailure(TicketFailure.Kind.UNAUTHORIZED, "SESSION_EXPIRED", message, cause);
        }
    }

    private TicketFailure mapFailure(int status, String body) {
        ApiError error = null;
        try {
            error = objectMapper.readValue(body, ApiError.class);
        } catch (RuntimeException ignored) {
            // A safe status-specific fallback is used below.
        }
        var code = error == null || error.code() == null ? "HTTP_" + status : error.code();
        var message = error == null || error.message() == null || error.message().isBlank()
                ? fallbackMessage(status) : error.message();
        var kind = switch (status) {
            case 400 -> TicketFailure.Kind.INVALID_REQUEST;
            case 401 -> TicketFailure.Kind.UNAUTHORIZED;
            case 403 -> TicketFailure.Kind.FORBIDDEN;
            case 404 -> TicketFailure.Kind.NOT_FOUND;
            case 409 -> TicketFailure.Kind.CONFLICT;
            default -> TicketFailure.Kind.SERVER;
        };
        return new TicketFailure(kind, code, message);
    }

    private String fallbackMessage(int status) {
        return status >= 500
                ? "ResolveIT is temporarily unavailable. Please try again."
                : "The request could not be completed. Please try again.";
    }

    private URI ticketUrl(int ticketId) {
        return URI.create(ticketsUrl + "/" + ticketId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String queryParameter(String name, Object value) {
        return value == null ? "" : "&" + name + "=" + encode(value.toString());
    }

    private static AuthenticatedSession fixedSession(SessionState session) {
        return new AuthenticatedSession() {
            @Override
            public CompletionStage<Session> validSession() {
                return session.current()
                        .<CompletionStage<Session>>map(CompletableFuture::completedFuture)
                        .orElseGet(() -> CompletableFuture.failedFuture(new TicketFailure(
                                TicketFailure.Kind.UNAUTHORIZED, "SESSION_EXPIRED",
                                "Your session has ended. Please sign in again.")));
            }

            @Override
            public CompletionStage<Session> refreshSession() {
                return CompletableFuture.failedFuture(new TicketFailure(
                        TicketFailure.Kind.UNAUTHORIZED, "SESSION_EXPIRED",
                        "Your session has ended. Please sign in again."));
            }
        };
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private record ApiError(int status, String code, String message) {}
}
