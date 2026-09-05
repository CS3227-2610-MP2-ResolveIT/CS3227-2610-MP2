package resolveit.frontend.config;

import java.net.URI;
import java.util.Map;

public record AppConfig(URI apiBaseUrl) {
    private static final String API_URL_ENVIRONMENT_VARIABLE = "RESOLVEIT_API_BASE_URL";
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/v1/";

    public AppConfig {
        if (apiBaseUrl == null || apiBaseUrl.getScheme() == null || apiBaseUrl.getHost() == null) {
            throw new IllegalArgumentException("The API base URL must be an absolute HTTP(S) URL.");
        }
        if (!"http".equalsIgnoreCase(apiBaseUrl.getScheme())
                && !"https".equalsIgnoreCase(apiBaseUrl.getScheme())) {
            throw new IllegalArgumentException("The API base URL must use HTTP or HTTPS.");
        }
        if (apiBaseUrl.getUserInfo() != null) {
            throw new IllegalArgumentException("The API base URL must not contain credentials.");
        }
    }

    public static AppConfig fromEnvironment(Map<String, String> environment) {
        var configured = environment.getOrDefault(API_URL_ENVIRONMENT_VARIABLE, DEFAULT_API_URL).trim();
        if (!configured.endsWith("/")) {
            configured += "/";
        }
        return new AppConfig(URI.create(configured));
    }
}
