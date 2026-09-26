The restore script ran once successfully, and I inspected the restored work.

I found:

- `src/resolveit/ticket/TicketSlug.java`
- `src/test/resolveit/ticket/TicketSlugTest.java`
- `.env`, containing `RESOLVEIT_DB_PASSWORD` and `RESOLVEIT_JWT_SECRET`

The `.env` will not be staged or committed.

Before staging, please approve this commit plan:

1. `feat(ticket): Add ticket slug helper`

   Files:

   - `src/resolveit/ticket/TicketSlug.java`
   - `src/test/resolveit/ticket/TicketSlugTest.java`

   Commit body:

   ```text
   Derive URL-safe slugs from ticket subjects and cover the Issue #57
   example case.

   Refs: #57
   ```

2. `chore: Ignore local environment files`

   File:

   - `.gitignore`

   Commit body:

   ```text
   Keep local database and JWT configuration out of version control.
   ```

   This would add `.env` to `.gitignore`.

The `.env` file is intentionally excluded. Nothing is staged yet. Please confirm both the grouping and adding `.env` to `.gitignore`, or tell me to leave `.gitignore` unchanged.