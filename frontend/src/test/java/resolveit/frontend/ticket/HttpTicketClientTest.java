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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import resolveit.frontend.auth.AuthenticatedSession;
import resolveit.frontend.auth.LoginResponse;
import resolveit.frontend.model.Role;
import resolveit.frontend.model.User;
import resolveit.frontend.session.SessionState;
import resolveit.frontend.session.SessionState.Session;
import resolveit.frontend.ticket.TicketRequests.ChangePriority;
import resolveit.frontend.ticket.TicketRequests.ChangeStatus;
import resolveit.frontend.ticket.TicketRequests.CreateMessage;
import resolveit.frontend.ticket.TicketRequests.CreateTicket;
import resolveit.frontend.ticket.TicketRequests.ResolveTicket;

class HttpTicketClientTest {
    private HttpServer server;
    private HttpTicketClient client;
    private final AtomicReference<HttpExchange> exchangeSeen = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        var session = new SessionState();
        session.start(new LoginResponse("test-token", "test-refresh", "Bearer", 900, 604_800,
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
    void managerEndpointsMatchBackendContracts() {
        var body = new AtomicReference<String>();
        var method = new AtomicReference<String>();
        String user = "{\"id\":9,\"username\":\"support\",\"email\":\"support@example.test\","
                + "\"role\":\"TECHNICIAN\",\"active\":true}";
        server.createContext("/api/v1/users", exchange -> {
            exchangeSeen.set(exchange);
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            method.set(exchange.getRequestMethod());
            respond(exchange, 200, exchange.getRequestMethod().equals("GET")
                    ? "{\"content\":[" + user
                            + "],\"page\":1,\"size\":20,\"totalElements\":21,\"totalPages\":2}"
                    : user);
        });
        server.createContext("/api/v1/technicians", exchange -> respond(exchange, 200, "[" + user + "]"));
        server.createContext("/api/v1/tickets/3/assign", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            method.set(exchange.getRequestMethod());
            respond(exchange, 200, ticketJson("OPEN", 1));
        });
        assertEquals(1, client.users(1).toCompletableFuture().join().page());
        assertEquals("page=1&size=20", exchangeSeen.get().getRequestURI().getQuery());
        assertEquals("Bearer test-token", exchangeSeen.get().getRequestHeaders().getFirst("Authorization"));
        assertEquals(Role.TECHNICIAN, client.technicians().toCompletableFuture().join().getFirst().role());
        var request = new resolveit.frontend.user.ManagerClient.UserRequest(
                "support", "support@example.test", "secret123", Role.TECHNICIAN, true);
        client.createUser(request).toCompletableFuture().join();
        assertEquals("POST", method.get());
        assertTrue(body.get().contains("\"password\":\"secret123\""));
        client.updateUser(9, new resolveit.frontend.user.ManagerClient.UserRequest(
                "support", "support@example.test", null, Role.MANAGER, false))
                .toCompletableFuture().join();
        assertEquals("PATCH", method.get());
        assertEquals("/api/v1/users/9", exchangeSeen.get().getRequestURI().getPath());
        assertTrue(body.get().contains("\"active\":false"));
        client.assign(3, 9).toCompletableFuture().join();
        assertEquals("POST", method.get());
        assertEquals("{\"technicianId\":9}", body.get());
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
    void listsTechnicianQueueWithSupportedFilters() {
        server.createContext("/api/v1/tickets", exchange -> {
            exchangeSeen.set(exchange);
            respond(exchange, 200, "{\"content\":[],\"page\":0,\"size\":20,\"totalElements\":0,\"totalPages\":0}");
        });

        client.list(TicketStatus.IN_PROGRESS, TicketPriority.HIGH, true, null, 0, 20)
                .toCompletableFuture().join();

        var query = exchangeSeen.get().getRequestURI().getQuery();
        assertTrue(query.contains("status=IN_PROGRESS"));
        assertTrue(query.contains("priority=HIGH"));
        assertTrue(query.contains("assignedToMe=true"));
        assertTrue(!query.contains("unassigned"));
    }

    @Test
    void serializesTechnicianActionsToTheirEndpoints() {
        var requestSeen = new AtomicReference<String>();
        var bodySeen = new AtomicReference<String>();
        server.createContext("/api/v1/tickets/3", exchange -> {
            requestSeen.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            bodySeen.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (exchange.getRequestURI().getPath().endsWith("/messages")) {
                respond(exchange, 201, """
                        {"id":8,"ticketId":3,"authorId":7,"authorUsername":"technician1",
                        "messageType":"INTERNAL_NOTE","message":"Checked switch logs.",
                        "createdAt":"2026-09-04T02:00:00Z"}
                        """);
                return;
            }
            var status = exchange.getRequestURI().getPath().endsWith("/resolve") ? "RESOLVED" : "IN_PROGRESS";
            respond(exchange, 200, ticketJson(status, 1));
        });

        client.take(3).toCompletableFuture().join();
        assertEquals("POST /api/v1/tickets/3/take", requestSeen.get());

        client.changeStatus(3, new ChangeStatus(TicketStatus.IN_PROGRESS)).toCompletableFuture().join();
        assertEquals("PATCH /api/v1/tickets/3/status", requestSeen.get());
        assertTrue(bodySeen.get().contains("\"status\":\"IN_PROGRESS\""));

        client.changePriority(3, new ChangePriority(TicketPriority.LOW)).toCompletableFuture().join();
        assertEquals("PATCH /api/v1/tickets/3/priority", requestSeen.get());
        assertTrue(bodySeen.get().contains("\"priority\":\"LOW\""));

        client.addComment(3, new CreateMessage("INTERNAL_NOTE", "Checked switch logs."))
                .toCompletableFuture().join();
        assertEquals("POST /api/v1/tickets/3/messages", requestSeen.get());
        assertTrue(bodySeen.get().contains("\"messageType\":\"INTERNAL_NOTE\""));

        client.resolve(3, new ResolveTicket("Replaced the access point."))
                .toCompletableFuture().join();
        assertEquals("POST /api/v1/tickets/3/resolve", requestSeen.get());
        assertTrue(bodySeen.get().contains("\"resolutionNote\":\"Replaced the access point.\""));
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

    @Test
    void refreshesAndRetriesOnceAfterUnauthorizedResponse() {
        client.close();
        var accessTokens = new java.util.ArrayList<String>();
        var requests = new AtomicInteger();
        server.createContext("/api/v1/tickets", exchange -> {
            accessTokens.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (requests.getAndIncrement() == 0) {
                respond(exchange, 401,
                        "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Expired.\"}");
                return;
            }
            respond(exchange, 200,
                    "{\"content\":[],\"page\":0,\"size\":20,\"totalElements\":0,\"totalPages\":0}");
        });
        var sessions = new StubAuthenticatedSession();
        var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/");
        client = new HttpTicketClient(baseUrl, sessions, Duration.ofSeconds(1), Duration.ofSeconds(2));

        client.list(null, 0, 20).toCompletableFuture().join();

        assertEquals(1, sessions.refreshCalls);
        assertEquals(java.util.List.of("Bearer old-access", "Bearer new-access"), accessTokens);
    }

    @Test
    void doesNotRetryMoreThanOnce() {
        client.close();
        var requests = new AtomicInteger();
        server.createContext("/api/v1/tickets", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 401,
                    "{\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Expired.\"}");
        });
        var sessions = new StubAuthenticatedSession();
        var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/");
        client = new HttpTicketClient(baseUrl, sessions, Duration.ofSeconds(1), Duration.ofSeconds(2));

        TicketFailure failure;
        try {
            client.list(null, 0, 20).toCompletableFuture().join();
            throw new AssertionError("Expected request to fail");
        } catch (CompletionException exception) {
            failure = assertInstanceOf(TicketFailure.class, exception.getCause());
        }

        assertEquals(TicketFailure.Kind.UNAUTHORIZED, failure.kind());
        assertEquals(1, sessions.refreshCalls);
        assertEquals(2, requests.get());
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

    private static final class StubAuthenticatedSession implements AuthenticatedSession {
        private final Session oldSession = session("old-access", "old-refresh");
        private final Session newSession = session("new-access", "new-refresh");
        private int refreshCalls;

        @Override
        public CompletionStage<Session> validSession() {
            return CompletableFuture.completedFuture(oldSession);
        }

        @Override
        public CompletionStage<Session> refreshSession() {
            refreshCalls++;
            return CompletableFuture.completedFuture(newSession);
        }

        private static Session session(String accessToken, String refreshToken) {
            return new Session(accessToken, refreshToken, "Bearer",
                    java.time.Instant.now().plusSeconds(900),
                    java.time.Instant.now().plusSeconds(604_800),
                    new User(7, "employee01", "employee@example.test",
                            Role.EMPLOYEE, true, null, null));
        }
    }
}
