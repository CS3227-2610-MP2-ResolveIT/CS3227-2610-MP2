package resolveit.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {
    private static final String EMAIL = "employee@test.local";
    private static final String ADDRESS = "127.0.0.1";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-12T00:00:00Z"));

    @Test
    void blocksTheAttemptAfterTheFailureThresholdUntilDurationPasses() {
        var limiter = limiter(100);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, ADDRESS));
            limiter.recordFailure(EMAIL, ADDRESS);
        }

        var blocked = assertThrows(LoginRateLimitException.class,
                () -> limiter.checkAllowed(EMAIL, ADDRESS));
        assertEquals(900, blocked.retryAfterSeconds());

        clock.advance(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, ADDRESS));
    }

    @Test
    void keysAreIsolatedAndSuccessfulLoginClearsFailures() {
        var limiter = limiter(100);
        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordFailure(EMAIL, ADDRESS);
        }

        assertDoesNotThrow(() -> limiter.checkAllowed("other@test.local", ADDRESS));
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, "127.0.0.2"));
        limiter.recordSuccess(EMAIL, ADDRESS);
        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, ADDRESS));
    }

    @Test
    void failuresOutsideTheWindowStartANewCount() {
        var limiter = limiter(100);
        for (int attempt = 0; attempt < 4; attempt++) {
            limiter.recordFailure(EMAIL, ADDRESS);
        }

        clock.advance(Duration.ofMinutes(11));
        limiter.recordFailure(EMAIL, ADDRESS);

        assertDoesNotThrow(() -> limiter.checkAllowed(EMAIL, ADDRESS));
    }

    @Test
    void trackedKeysRemainBounded() {
        var limiter = limiter(2);

        limiter.recordFailure("first@test.local", ADDRESS);
        clock.advance(Duration.ofSeconds(1));
        limiter.recordFailure("second@test.local", ADDRESS);
        clock.advance(Duration.ofSeconds(1));
        limiter.recordFailure("third@test.local", ADDRESS);

        assertTrue(limiter.trackedAttempts() <= 2);
    }

    private LoginAttemptLimiter limiter(int maxEntries) {
        return new LoginAttemptLimiter(clock, 5, Duration.ofMinutes(10),
                Duration.ofMinutes(15), maxEntries);
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
