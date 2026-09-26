#!/usr/bin/env bash
set -euo pipefail

mkdir -p src/resolveit/ticket src/test/resolveit/ticket src/resolveit/config

cat > src/resolveit/ticket/PriorityWeights.java <<'EOF'
package resolveit.ticket;

/** Maps ticket priorities to a numeric weight for queue ordering. */
public final class PriorityWeights {
    private PriorityWeights() {}

    public static int weight(TicketPriority priority) {
        switch (priority) {
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                throw new IllegalArgumentException("Unknown priority: " + priority);
        }
    }
}
EOF

cat > src/test/resolveit/ticket/PriorityWeightsTest.java <<'EOF'
package resolveit.ticket;

public final class PriorityWeightsTest {
    public static void main(String[] args) {
        if (PriorityWeights.weight(TicketPriority.HIGH) <= PriorityWeights.weight(TicketPriority.LOW)) {
            throw new AssertionError("HIGH must outrank LOW");
        }
    }
}
EOF

cat > src/resolveit/config/logging.properties <<'EOF'
# Unrelated maintenance: quiet the noisy Spring Security request logger.
logging.level.org.springframework.security=WARN
logging.level.resolveit.ticket=INFO
EOF
