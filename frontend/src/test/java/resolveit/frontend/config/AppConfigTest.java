package resolveit.frontend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppConfigTest {
    @Test
    void usesLocalBackendByDefault() {
        assertEquals(URI.create("http://localhost:8080/api/v1/"),
                AppConfig.fromEnvironment(Map.of()).apiBaseUrl());
    }

    @Test
    void normalizesConfiguredBaseUrl() {
        var config = AppConfig.fromEnvironment(Map.of(
                "RESOLVEIT_API_BASE_URL", "https://support.example.test/api/v1"));

        assertEquals(URI.create("https://support.example.test/api/v1/"), config.apiBaseUrl());
    }

    @Test
    void rejectsNonHttpUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> AppConfig.fromEnvironment(Map.of("RESOLVEIT_API_BASE_URL", "file:///tmp/api")));
    }
}
