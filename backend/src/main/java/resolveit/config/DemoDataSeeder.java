package resolveit.config;

import java.util.ArrayList;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import resolveit.user.Role;
import resolveit.user.User;
import resolveit.user.UserRepository;

@Component
@ConditionalOnProperty(name = "resolveit.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() != 0) return;

        var demoUsers = new ArrayList<User>();
        var managerPassword = passwordEncoder.encode("Manager123!");
        var technicianPassword = passwordEncoder.encode("Technician123!");
        var employeePassword = passwordEncoder.encode("Employee123!");
        demoUsers.add(new User("manager", "manager@resolveit.local",
                managerPassword, Role.MANAGER, true));
        for (int i = 1; i <= 3; i++) {
            demoUsers.add(new User("technician" + i, "technician" + i + "@resolveit.local",
                    technicianPassword, Role.TECHNICIAN, true));
        }
        for (int i = 1; i <= 30; i++) {
            var suffix = "%02d".formatted(i);
            demoUsers.add(new User("employee" + suffix, "employee" + suffix + "@resolveit.local",
                    employeePassword, Role.EMPLOYEE, true));
        }
        users.saveAll(demoUsers);
    }
}
