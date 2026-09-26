#!/usr/bin/env bash
set -euo pipefail

mkdir -p src/resolveit/ticket src/test/resolveit/ticket docs

cat > src/resolveit/ticket/PriorityBadge.java <<'EOF'
package resolveit.ticket;

/** Renders a ticket priority as a coloured badge for the ticket list. */
public final class PriorityBadge {
    private PriorityBadge() {}

    public static String render(TicketPriority priority) {
        switch (priority) {
            case HIGH:
                return "\uD83D\uDD34 HIGH";
            case MEDIUM:
                return "\uD83D\uDFE1 MEDIUM";
            case LOW:
                return "\uD83D\uDFE2 LOW";
            default:
                throw new IllegalArgumentException("Unknown priority: " + priority);
        }
    }
}
EOF

cat > src/test/resolveit/ticket/PriorityBadgeTest.java <<'EOF'
package resolveit.ticket;

public final class PriorityBadgeTest {
    public static void main(String[] args) {
        if (!PriorityBadge.render(TicketPriority.HIGH).contains("HIGH")) {
            throw new AssertionError("HIGH badge should contain its label");
        }
    }
}
EOF

# The user-guide line documents the behaviour this same change delivers, so it
# is part of one inseparable feature outcome, not a separate documentation task.
cat >> docs/UserGuide.md <<'EOF'

Ticket priority is shown as a coloured badge, for example `🔴 HIGH`.
EOF
