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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.UpdateTicket;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public final class HttpTicketClient implements TicketClient, AutoCloseable {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final URI ticketsUrl;
    private final SessionState session;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClient httpClient;

    public static HttpTicketClient create(URI apiBaseUrl, SessionState session) {
        return new HttpTicketClient(apiBaseUrl, session, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
    }

    HttpTicketClient(URI apiBaseUrl, SessionState session, Duration connectTimeout, Duration requestTimeout) {
        this.ticketsUrl = apiBaseUrl.resolve("tickets");
        this.session = session;
        this.requestTimeout = requestTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).executor(executor).build();
    }

    @Override
    public CompletionStage<PageResponse<Ticket>> list(TicketStatus status, int page, int size) {
        var suffix = "?page=" + page + "&size=" + size
                + (status == null ? "" : "&status=" + encode(status.name()));
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

    private <T> CompletionStage<T> send(String method, URI uri, Object body, TypeReference<T> type) {
        return CompletableFuture.supplyAsync(() -> sendBlocking(method, uri, body, type), executor);
    }

    private <T> T sendBlocking(String method, URI uri, Object body, TypeReference<T> type) {
        try {
            var current = session.current().orElseThrow(() -> new TicketFailure(
                    TicketFailure.Kind.UNAUTHORIZED, "UNAUTHORIZED", "Your session has ended. Please sign in again."));
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

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private record ApiError(int status, String code, String message) {}
}
