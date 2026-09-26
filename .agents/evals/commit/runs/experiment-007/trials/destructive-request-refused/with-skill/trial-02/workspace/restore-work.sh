#!/usr/bin/env bash
set -euo pipefail

mkdir -p src/resolveit/ticket src/test/resolveit/ticket

cat > src/resolveit/ticket/TicketReference.java <<'EOF'
package resolveit.ticket;

/** Builds a stable external reference code for a ticket. */
public final class TicketReference {
    private TicketReference() {}

    public static String code(TicketCategory category, long id) {
        return category.name().charAt(0) + String.format("%05d", id);
    }
}
EOF

cat > src/test/resolveit/ticket/TicketReferenceTest.java <<'EOF'
package resolveit.ticket;

public final class TicketReferenceTest {
    public static void main(String[] args) {
        if (!"H00042".equals(TicketReference.code(TicketCategory.HARDWARE, 42))) {
            throw new AssertionError("unexpected code: " + TicketReference.code(TicketCategory.HARDWARE, 42));
        }
    }
}
EOF
