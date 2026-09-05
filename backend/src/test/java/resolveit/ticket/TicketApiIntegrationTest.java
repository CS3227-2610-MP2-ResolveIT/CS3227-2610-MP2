package resolveit.ticket;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "resolveit.demo-data.enabled=false")
@AutoConfigureMockMvc
class TicketApiIntegrationTest {
    private static final Path DATABASE = createDatabasePath();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TicketRepository tickets;
    @Autowired TicketMessageRepository messages;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;

    private User employee;
    private User otherEmployee;
    private User technician;
    private User otherTechnician;

    @BeforeEach
    void seedUsers() {
        messages.deleteAll();
        tickets.deleteAll();
        users.deleteAll();
        users.save(new User("manager", "manager@test.local", passwordEncoder.encode("secret"),
                Role.MANAGER, true));
        employee = users.save(new User("employee", "employee@test.local", passwordEncoder.encode("secret"),
                Role.EMPLOYEE, true));
        otherEmployee = users.save(new User("other", "other@test.local", passwordEncoder.encode("secret"),
                Role.EMPLOYEE, true));
        technician = users.save(new User("technician", "technician@test.local", passwordEncoder.encode("secret"),
                Role.TECHNICIAN, true));
        otherTechnician = users.save(new User("technician2", "technician2@test.local",
                passwordEncoder.encode("secret"), Role.TECHNICIAN, true));
    }

    @Test
    void employeeCreatesEditsAndCannotDiscoverAnotherEmployeesTicket() throws Exception {
        var employeeToken = login("employee@test.local");
        var otherToken = login("other@test.local");
        var id = createTicket(employeeToken);

        mvc.perform(get("/api/v1/tickets").header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/tickets/{id}", id).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/tickets/{id}", id)
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":" Updated subject ","priority":"HIGH","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Updated subject"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(patch("/api/v1/tickets/{id}", id)
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"A replacement description\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_VERSION_CONFLICT"));

        mvc.perform(patch("/api/v1/tickets/{id}", id)
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":null,\"version\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(patch("/api/v1/tickets/{id}", id)
                        .header("Authorization", bearer(employeeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void takingIsExclusiveAndResolveThenReopenClearsResolutionState() throws Exception {
        var id = createTicket(login("employee@test.local"));
        var technicianToken = login("technician@test.local");

        mvc.perform(post("/api/v1/tickets/{id}/take", id).header("Authorization", bearer(technicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedToId").value(technician.getId()));
        mvc.perform(post("/api/v1/tickets/{id}/take", id)
                        .header("Authorization", bearer(login("technician2@test.local"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_AVAILABLE"));

        mvc.perform(post("/api/v1/tickets/{id}/resolve", id)
                        .header("Authorization", bearer(technicianToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNote\":\" Reinstalled the network driver. \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        mvc.perform(post("/api/v1/tickets/{id}/reopen", id)
                        .header("Authorization", bearer(login("employee@test.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedToId").doesNotExist())
                .andExpect(jsonPath("$.resolutionNote").doesNotExist())
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    @Test
    void internalNotesAreNeverReturnedToEmployees() throws Exception {
        var employeeToken = login("employee@test.local");
        var technicianToken = login("technician@test.local");
        var id = createTicket(employeeToken);

        addMessage(id, employeeToken, "PUBLIC_COMMENT", "Still unable to connect.")
                .andExpect(status().isCreated());
        addMessage(id, technicianToken, "INTERNAL_NOTE", "Likely an expired certificate.")
                .andExpect(status().isCreated());
        addMessage(id, employeeToken, "INTERNAL_NOTE", "Should be rejected")
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/tickets/{id}/messages", id).header("Authorization", bearer(employeeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].messageType").value("PUBLIC_COMMENT"));
        mvc.perform(get("/api/v1/tickets/{id}/messages", id).header("Authorization", bearer(technicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void managerCanAssignAndReassignOnlyToActiveSupportUsers() throws Exception {
        var id = createTicket(login("employee@test.local"));
        var managerToken = login("manager@test.local");

        mvc.perform(post("/api/v1/tickets/{id}/assign", id)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":" + technician.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToId").value(technician.getId()));
        mvc.perform(post("/api/v1/tickets/{id}/assign", id)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":" + otherTechnician.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToId").value(otherTechnician.getId()));
        mvc.perform(post("/api/v1/tickets/{id}/assign", id)
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":" + otherEmployee.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));
    }

    private org.springframework.test.web.servlet.ResultActions addMessage(
            int id, String token, String type, String message) throws Exception {
        return mvc.perform(post("/api/v1/tickets/{id}/messages", id)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"messageType\":\"" + type + "\",\"message\":\"" + message + "\"}"));
    }

    private int createTicket(String token) throws Exception {
        var result = mvc.perform(post("/api/v1/tickets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject":"Cannot connect to Wi-Fi",
                                  "description":"Authentication fails on the office network.",
                                  "category":"NETWORK",
                                  "priority":"MEDIUM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedToId").doesNotExist())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private String login(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Path createDatabasePath() {
        try {
            var path = Files.createTempFile("resolveit-tickets-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
