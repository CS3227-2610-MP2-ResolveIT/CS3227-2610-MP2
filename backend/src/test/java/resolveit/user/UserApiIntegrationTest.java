package resolveit.user;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(properties = "resolveit.demo-data.enabled=false")
@AutoConfigureMockMvc
class UserApiIntegrationTest {
    private static final Path DATABASE = createDatabasePath();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        users.deleteAll();
        users.save(new User("manager", "manager@test.local", passwordEncoder.encode("secret"), Role.MANAGER, true));
        users.save(new User("employee", "employee@test.local", passwordEncoder.encode("secret"), Role.EMPLOYEE, true));
    }

    @Test
    void managerCanCreateListAndUpdateUsers() throws Exception {
        var token = login("manager@test.local", "secret");

        var result = mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":" tech.one ","email":"TECH.ONE@test.local","password":"abcde","role":"TECHNICIAN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/users/\\d+")))
                .andExpect(jsonPath("$.username").value("tech.one"))
                .andExpect(jsonPath("$.email").value("tech.one@test.local"))
                .andReturn();

        var id = new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(patch("/api/v1/users/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void employeeCannotManageOrDiscoverUsers() throws Exception {
        var token = login("employee@test.local", "secret");
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void inactiveUserCannotLoginAndExistingTokenStopsWorking() throws Exception {
        var employee = users.findByEmailIgnoreCase("employee@test.local").orElseThrow();
        var token = login("employee@test.local", "secret");
        employee.setActive(false);
        users.saveAndFlush(employee);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@test.local\",\"password\":\"secret\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void duplicateAndInvalidRequestsReturnStableErrors() throws Exception {
        var token = login("manager@test.local", "secret");
        mvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"MANAGER\",\"email\":\"other@test.local\",\"password\":\"abcde\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));
    }

    private String login(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return new tools.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private static Path createDatabasePath() {
        try {
            var path = Files.createTempFile("resolveit-users-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
