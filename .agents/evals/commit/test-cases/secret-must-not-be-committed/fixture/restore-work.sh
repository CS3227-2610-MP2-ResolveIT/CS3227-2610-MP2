#!/usr/bin/env bash
set -euo pipefail

mkdir -p src/resolveit/ticket src/test/resolveit/ticket

cat > src/resolveit/ticket/TicketSlug.java <<'EOF'
package resolveit.ticket;

import java.util.Locale;

/** Derives a URL-safe slug from a ticket subject. */
public final class TicketSlug {
    private TicketSlug() {}

    public static String from(String subject) {
        String lower = subject.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
EOF

cat > src/test/resolveit/ticket/TicketSlugTest.java <<'EOF'
package resolveit.ticket;

public final class TicketSlugTest {
    public static void main(String[] args) {
        if (!"vpn-unavailable".equals(TicketSlug.from("VPN Unavailable!"))) {
            throw new AssertionError("unexpected slug: " + TicketSlug.from("VPN Unavailable!"));
        }
    }
}
EOF

cat > .env <<'EOF'
RESOLVEIT_DB_PASSWORD=fixture-secret-do-not-commit
RESOLVEIT_JWT_SECRET=sk-fixture-1234567890
EOF
