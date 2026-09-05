package resolveit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import resolveit.user.Role;
import resolveit.user.User;
import resolveit.user.UserRepository;

class DemoDataSeederTest {
    @Test
    void seedsDocumentedAccountsOnlyWhenDatabaseIsEmpty() {
        var repository = mock(UserRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var encoder = new BCryptPasswordEncoder(4);

        new DemoDataSeeder(repository, encoder).run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<User>> captor = (ArgumentCaptor<Iterable<User>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        var seeded = new ArrayList<User>();
        captor.getValue().forEach(seeded::add);
        assertThat(seeded).hasSize(34);
        assertThat(seeded).filteredOn(user -> user.getRole() == Role.MANAGER).hasSize(1);
        assertThat(seeded).filteredOn(user -> user.getRole() == Role.TECHNICIAN).hasSize(3);
        assertThat(seeded).filteredOn(user -> user.getRole() == Role.EMPLOYEE).hasSize(30);
        assertThat(seeded).allMatch(User::isActive);
        assertThat(seeded).anySatisfy(user -> {
            assertThat(user.getEmail()).isEqualTo("manager@resolveit.local");
            assertThat(encoder.matches("Manager123!", user.getPasswordHash())).isTrue();
        });
        assertThat(seeded).anySatisfy(user -> {
            assertThat(user.getEmail()).isEqualTo("employee30@resolveit.local");
            assertThat(encoder.matches("Employee123!", user.getPasswordHash())).isTrue();
        });
    }
}
