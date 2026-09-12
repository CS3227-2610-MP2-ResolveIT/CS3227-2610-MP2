package resolveit.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Applies a bounded, process-local limit to repeated credential failures. */
@Component
public class LoginAttemptLimiter {
    private final Clock clock;
    private final int maxFailures;
    private final Duration window;
    private final Duration blockDuration;
    private final int maxEntries;
    private final Map<LoginKey, AttemptState> attempts = new HashMap<>();

    /**
     * Creates a bounded, process-local login limiter.
     *
     * @param clock limiter clock
     * @param maxFailures failures permitted before temporary blocking
     * @param window duration in which failures accumulate
     * @param blockDuration duration of a temporary block
     * @param maxEntries maximum process-local keys retained
     */
    public LoginAttemptLimiter(
            Clock clock,
            @Value("${resolveit.auth.login-rate-limit.max-failures}") int maxFailures,
            @Value("${resolveit.auth.login-rate-limit.window}") Duration window,
            @Value("${resolveit.auth.login-rate-limit.block-duration}") Duration blockDuration,
            @Value("${resolveit.auth.login-rate-limit.max-entries}") int maxEntries) {
        if (maxFailures <= 0 || maxEntries <= 0 || window.isZero() || window.isNegative()
                || blockDuration.isZero() || blockDuration.isNegative()) {
            throw new IllegalArgumentException("Login rate-limit settings must be positive.");
        }
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.window = window;
        this.blockDuration = blockDuration;
        this.maxEntries = maxEntries;
    }

    /**
     * Rejects a currently blocked login key.
     *
     * @param email submitted account email
     * @param remoteAddress direct client address
     */
    public synchronized void checkAllowed(String email, String remoteAddress) {
        var now = clock.instant();
        cleanup(now);
        var state = attempts.get(LoginKey.from(email, remoteAddress));
        if (state != null && state.blockedUntil() != null && state.blockedUntil().isAfter(now)) {
            throw new LoginRateLimitException(secondsUntil(now, state.blockedUntil()));
        }
    }

    /**
     * Records one generic credential failure without revealing account state.
     *
     * @param email submitted account email
     * @param remoteAddress direct client address
     */
    public synchronized void recordFailure(String email, String remoteAddress) {
        var now = clock.instant();
        cleanup(now);
        var key = LoginKey.from(email, remoteAddress);
        if (!attempts.containsKey(key) && attempts.size() >= maxEntries) {
            removeOldestEntry();
        }

        var previous = attempts.get(key);
        var failures = previous == null ? 1 : previous.failures() + 1;
        var windowStarted = previous == null ? now : previous.windowStarted();
        var blockedUntil = failures >= maxFailures ? now.plus(blockDuration) : null;
        attempts.put(key, new AttemptState(failures, windowStarted, blockedUntil, now));
    }

    /**
     * Clears failures after a successful login.
     *
     * @param email authenticated account email
     * @param remoteAddress direct client address
     */
    public synchronized void recordSuccess(String email, String remoteAddress) {
        attempts.remove(LoginKey.from(email, remoteAddress));
    }

    synchronized int trackedAttempts() {
        return attempts.size();
    }

    private void cleanup(Instant now) {
        attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
    }

    private boolean isExpired(AttemptState state, Instant now) {
        if (state.blockedUntil() != null) {
            return !state.blockedUntil().isAfter(now);
        }
        return !state.windowStarted().plus(window).isAfter(now);
    }

    private void removeOldestEntry() {
        attempts.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        java.util.Comparator.comparing(AttemptState::lastSeen)))
                .map(Map.Entry::getKey)
                .ifPresent(attempts::remove);
    }

    private static long secondsUntil(Instant now, Instant blockedUntil) {
        var millis = Duration.between(now, blockedUntil).toMillis();
        return Math.max(1, (millis + 999) / 1_000);
    }

    private record LoginKey(String email, String remoteAddress) {
        private static LoginKey from(String email, String remoteAddress) {
            var normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
            var normalizedAddress = remoteAddress == null || remoteAddress.isBlank()
                    ? "unknown" : remoteAddress.trim();
            return new LoginKey(normalizedEmail, normalizedAddress);
        }
    }

    private record AttemptState(int failures, Instant windowStarted,
                                Instant blockedUntil, Instant lastSeen) {}
}
