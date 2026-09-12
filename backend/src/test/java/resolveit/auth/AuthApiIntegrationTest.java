package resolveit.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import resolveit.user.Role;
import resolveit.user.User;
import resolveit.user.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "resolveit.demo-data.enabled=false")
@AutoConfigureMockMvc
class AuthApiIntegrationTest {
    private static final Path DATABASE = createDatabasePath();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedUsers() {
        refreshTokens.deleteAll();
        users.deleteAll();
        users.save(new User("manager", "manager@test.local",
                passwordEncoder.encode("secret"), Role.MANAGER, true));
        users.save(new User("employee", "employee@test.local",
                passwordEncoder.encode("secret"), Role.EMPLOYEE, true));
    }

    @Test
    void loginIssuesAccessAndHashedRefreshTokens() throws Exception {
        var login = login("employee@test.local", "secret");

        var rawRefreshToken = login.get("refreshToken").asString();
        assertTrue(login.get("accessToken").asString().length() > 20);
        assertTrue(rawRefreshToken.length() >= 40);
        assertEquals("Bearer", login.get("tokenType").asString());
        assertEquals(900, login.get("expiresIn").asLong());
        assertEquals(604_800, login.get("refreshExpiresIn").asLong());

        var stored = refreshTokens.findAll().getFirst();
        assertNotEquals(rawRefreshToken, stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());
        assertEquals(login.get("user").get("id").asInt(), stored.getUser().getId());
    }

    @Test
    void refreshRotatesBothTokensAndRejectsReuse() throws Exception {
        var login = login("employee@test.local", "secret");
        var oldAccessToken = login.get("accessToken").asString();
        var oldRefreshToken = login.get("refreshToken").asString();

        var refreshed = refresh(oldRefreshToken, 200);

        assertNotEquals(oldAccessToken, refreshed.get("accessToken").asString());
        assertNotEquals(oldRefreshToken, refreshed.get("refreshToken").asString());
        refresh(oldRefreshToken, 401);
        refresh(refreshed.get("refreshToken").asString(), 200);
    }

    @Test
    void expiredAndUnknownRefreshTokensAreRejectedSafely() throws Exception {
        var user = users.findByEmailIgnoreCase("employee@test.local").orElseThrow();
        var rawExpiredToken = "expired-test-token";
        refreshTokens.save(new RefreshToken(user, hash(rawExpiredToken),
                Instant.now().minusSeconds(1), Instant.now().minusSeconds(2)));

        refresh(rawExpiredToken, 401);
        refresh("unknown-refresh-token", 401);
    }

    @Test
    void inactiveUserCannotLoginOrRefresh() throws Exception {
        var login = login("employee@test.local", "secret");
        var employee = users.findByEmailIgnoreCase("employee@test.local").orElseThrow();
        employee.setActive(false);
        users.saveAndFlush(employee);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@test.local\",\"password\":\"secret\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        refresh(login.get("refreshToken").asString(), 401);
    }

    @Test
    void logoutRevokesOnlyTheAuthenticatedUsersTokenAndIsIdempotent() throws Exception {
        var employeeLogin = login("employee@test.local", "secret");
        var managerLogin = login("manager@test.local", "secret");
        var employeeRefreshToken = employeeLogin.get("refreshToken").asString();

        logout(managerLogin.get("accessToken").asString(), employeeRefreshToken, 204);
        var employeeRefresh = refresh(employeeRefreshToken, 200);

        var currentRefreshToken = employeeRefresh.get("refreshToken").asString();
        logout(employeeLogin.get("accessToken").asString(), currentRefreshToken, 204);
        logout(employeeLogin.get("accessToken").asString(), currentRefreshToken, 204);
        refresh(currentRefreshToken, 401);
    }

    @Test
    void oneRefreshTokenCannotBeConsumedTwiceConcurrently() throws Exception {
        var login = login("employee@test.local", "secret");
        var refreshToken = login.get("refreshToken").asString();
        var start = new CountDownLatch(1);
        Callable<Integer> request = () -> {
            start.await();
            return mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody(refreshToken)))
                    .andReturn().getResponse().getStatus();
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            start.countDown();
            var statuses = java.util.List.of(first.get(), second.get()).stream().sorted().toList();
            assertEquals(java.util.List.of(200, 401), statuses);
        }
    }

    private JsonNode login(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        if (expectedStatus == 200) {
            return objectMapper.readTree(result.getResponse().getContentAsString());
        }
        assertEquals("INVALID_REFRESH_TOKEN",
                objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asString());
        return null;
    }

    private void logout(String accessToken, String refreshToken, int expectedStatus) throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().is(expectedStatus));
    }

    private static String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private static String hash(String rawToken) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }

    private static Path createDatabasePath() {
        try {
            var path = Files.createTempFile("resolveit-auth-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
