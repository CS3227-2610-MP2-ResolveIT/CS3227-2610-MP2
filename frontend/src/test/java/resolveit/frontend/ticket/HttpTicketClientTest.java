package resolveit.frontend.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;

class HttpTicketClientTest {
    private HttpServer server;
    private HttpTicketClient client;
    private final AtomicReference<HttpExchange> exchangeSeen = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        var session = new SessionState();
        session.start(new LoginResponse("test-token", "Bearer", 900,
                new User(7, "employee01", "employee@example.test", Role.EMPLOYEE, true, null, null)));
        var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/");
        client = new HttpTicketClient(baseUrl, session, Duration.ofSeconds(1), Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop(0);
    }

    @Test
    void listsTicketsWithStatusAndBearerToken() {
        server.createContext("/api/v1/tickets", exchange -> {
            exchangeSeen.set(exchange);
            respond(exchange, 200, """
                    {"content":[{"id":3,"ticketNumber":"TKT-003","subject":"Office Wi-Fi unavailable",
                    "description":"Cannot connect from my laptop.","category":"NETWORK","priority":"HIGH",
                    "status":"OPEN","requesterId":7,"requesterUsername":"employee01","assignedToId":null,
                    "assignedToUsername":null,"resolutionNote":null,"createdAt":"2026-09-04T01:00:00Z",
                    "updatedAt":"2026-09-04T01:00:00Z","resolvedAt":null,"version":0}],
                    "page":0,"size":100,"totalElements":1,"totalPages":1}
                    """);
        });

        var page = client.list(TicketStatus.OPEN, 0, 20).toCompletableFuture().join();

        assertEquals(1, page.content().size());
        assertEquals(TicketCategory.NETWORK, page.content().getFirst().category());
        assertTrue(exchangeSeen.get().getRequestURI().getQuery().contains("status=OPEN"));
        assertTrue(exchangeSeen.get().getRequestURI().getQuery().contains("size=20"));
        assertEquals("Bearer test-token", exchangeSeen.get().getRequestHeaders().getFirst("Authorization"));
    }

    @Test
    void serializesTicketCreation() {
        var bodySeen = new AtomicReference<String>();
        server.createContext("/api/v1/tickets", exchange -> {
            bodySeen.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 201, ticketJson("OPEN", 0));
        });

        var created = client.create(new CreateTicket("Office Wi-Fi unavailable", "Cannot connect from my laptop.",
                TicketCategory.NETWORK, TicketPriority.HIGH)).toCompletableFuture().join();

        assertEquals("TKT-003", created.ticketNumber());
        assertTrue(bodySeen.get().contains("\"category\":\"NETWORK\""));
        assertTrue(bodySeen.get().contains("\"priority\":\"HIGH\""));
    }

    @Test
    void mapsConflictCodeAndMessage() {
        server.createContext("/api/v1/tickets/3", exchange -> respond(exchange, 409,
                "{\"status\":409,\"code\":\"TICKET_VERSION_CONFLICT\",\"message\":\"Ticket changed.\"}"));

        TicketFailure failure;
        try {
            client.get(3).toCompletableFuture().join();
            throw new AssertionError("Expected request to fail");
        } catch (CompletionException exception) {
            failure = assertInstanceOf(TicketFailure.class, exception.getCause());
        }

        assertEquals(TicketFailure.Kind.CONFLICT, failure.kind());
        assertEquals("TICKET_VERSION_CONFLICT", failure.code());
        assertEquals("Ticket changed.", failure.getMessage());
    }

    private static String ticketJson(String status, int version) {
        return """
                {"id":3,"ticketNumber":"TKT-003","subject":"Office Wi-Fi unavailable",
                "description":"Cannot connect from my laptop.","category":"NETWORK","priority":"HIGH",
                "status":"%s","requesterId":7,"requesterUsername":"employee01","assignedToId":null,
                "assignedToUsername":null,"resolutionNote":null,"createdAt":"2026-09-04T01:00:00Z",
                "updatedAt":"2026-09-04T01:00:00Z","resolvedAt":null,"version":%d}
                """.formatted(status, version);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
