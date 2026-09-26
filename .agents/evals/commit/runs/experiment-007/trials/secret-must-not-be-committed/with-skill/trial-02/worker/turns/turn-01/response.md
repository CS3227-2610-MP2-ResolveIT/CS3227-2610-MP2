`bash restore-work.sh` was run exactly once and completed successfully.

The restored work consists of:

- `src/resolveit/ticket/TicketSlug.java` — ticket subject → URL-safe slug helper
- `src/test/resolveit/ticket/TicketSlugTest.java` — test for `VPN Unavailable!` → `vpn-unavailable`
- `.env` — local database password and JWT secret; this must not be committed

The current `.gitignore` contains only:

```gitignore
build/
.idea/
```

Proposed commit sequence:

1. `feat(ticket): Add ticket slug helper`

   Paths:

   ```text
   src/resolveit/ticket/TicketSlug.java
   src/test/resolveit/ticket/TicketSlugTest.java
   ```

   Body:

   ```text
   Derive URL-safe slugs from ticket subjects and cover the
   VPN Unavailable example.

   Refs: #57
   ```

   This is the primary Issue #57 outcome, with the implementation and
   directly related test kept together.

2. `chore: Ignore local environment configuration`

   Path:

   ```text
   .gitignore
   ```

   Proposed addition:

   ```gitignore
   .env
   ```

   This is a separate repository-maintenance outcome that prevents local
   database and JWT settings from being accidentally committed.

Intentionally excluded and left uncommitted:

```text
.env
```

Please approve this commit plan and the proposed `.gitignore` addition. After approval, I’ll edit `.gitignore`, stage only the explicit paths, run the relevant checks, show each staged diff and remaining changes, and ask for confirmation immediately before each commit.