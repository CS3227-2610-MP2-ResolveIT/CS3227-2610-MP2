package resolveit.frontend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpAuthClientTest {
    private HttpServer server;
    private HttpAuthClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void serializesRequestAndMapsSuccessfulResponse() throws Exception {
        var requestBody = new AtomicReference<String>();
        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"accessToken":"abc123","tokenType":"Bearer","expiresIn":900,
                     "user":{"id":1,"username":"manager","email":"manager@resolveit.local",
                     "role":"MANAGER","active":true,"createdAt":"2026-09-04T00:00:00Z",
                     "updatedAt":"2026-09-04T00:00:00Z"}}
                    """);
        });

        var response = client.login(new LoginRequest("manager@resolveit.local", "secret"))
                .toCompletableFuture().join();

        assertEquals("abc123", response.accessToken());
        assertEquals("manager", response.user().username());
        assertTrue(requestBody.get().contains("\"email\":\"manager@resolveit.local\""));
        assertTrue(requestBody.get().contains("\"password\":\"secret\""));
    }

    @Test
    void mapsInvalidCredentialsToSafeFailure() throws Exception {
        startServer(exchange -> respond(exchange, 401,
                "{\"status\":401,\"code\":\"INVALID_CREDENTIALS\",\"message\":\"Incorrect.\"}"));

        var failure = failureFromLogin();

        assertEquals(AuthFailure.Kind.INVALID_CREDENTIALS, failure.kind());
        assertEquals("The email or password is incorrect.", failure.getMessage());
    }

    @Test
    void mapsValidationAndServerFailures() throws Exception {
        startServer(exchange -> respond(exchange, 400,
                "{\"status\":400,\"code\":\"VALIDATION_FAILED\",\"message\":\"Invalid fields: email.\"}"));
        assertEquals(AuthFailure.Kind.INVALID_REQUEST, failureFromLogin().kind());

        server.removeContext("/api/v1/auth/login");
        server.createContext("/api/v1/auth/login", exchange -> respond(exchange, 500, "not-json"));
        var serverFailure = failureFromLogin();
        assertEquals(AuthFailure.Kind.SERVER, serverFailure.kind());
        assertEquals("ResolveIT is temporarily unavailable. Please try again.", serverFailure.getMessage());
    }

    @Test
    void rejectsMalformedSuccessfulResponse() throws Exception {
        startServer(exchange -> respond(exchange, 200, "{\"accessToken\":null}"));

        assertEquals(AuthFailure.Kind.INVALID_RESPONSE, failureFromLogin().kind());
    }

    @Test
    void reportsRequestTimeout() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                respond(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }, Duration.ofMillis(75));

        assertEquals(AuthFailure.Kind.TIMEOUT, failureFromLogin().kind());
    }

    @Test
    void reportsUnavailableBackend() throws Exception {
        int unusedPort;
        try (var socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        var baseUrl = URI.create("http://127.0.0.1:" + unusedPort + "/api/v1/");
        client = new HttpAuthClient(baseUrl, Duration.ofMillis(250), Duration.ofSeconds(1));

        assertEquals(AuthFailure.Kind.CONNECTION, failureFromLogin().kind());
    }

    private AuthFailure failureFromLogin() {
        try {
            client.login(new LoginRequest("user@example.test", "secret"))
                    .toCompletableFuture().join();
            throw new AssertionError("Expected login to fail");
        } catch (CompletionException exception) {
            return assertInstanceOf(AuthFailure.class, exception.getCause());
        }
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        startServer(handler, Duration.ofSeconds(2));
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler, Duration requestTimeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/auth/login", handler);
        server.start();
        var baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/");
        client = new HttpAuthClient(baseUrl, Duration.ofSeconds(1), requestTimeout);
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
