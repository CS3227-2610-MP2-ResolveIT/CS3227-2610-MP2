package resolveit.frontend.user;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ManagerServiceTest {
    @Test void validatesCreationAndOptionalPasswordUpdates() {
        assertNull(ManagerService.validate(" manager ", "manager@example.test", "abcde", true));
        assertNotNull(ManagerService.validate("manager", "manager@example.test", "", true));
        assertNull(ManagerService.validate("manager", "manager@example.test", "", false));
        assertNotNull(ManagerService.validate("manager", "manager@example.test", "1234", false));
        assertNotNull(ManagerService.validate("manager", "manager@example.test", " ".repeat(5), true));
        assertNotNull(ManagerService.validate(" ab ", "manager@example.test", "abcde", true));
        assertNotNull(ManagerService.validate("manager", "invalid address@example.test", "abcde", true));
        assertNotNull(ManagerService.validate("manager", "no-domain", "abcde", true));
    }
}
