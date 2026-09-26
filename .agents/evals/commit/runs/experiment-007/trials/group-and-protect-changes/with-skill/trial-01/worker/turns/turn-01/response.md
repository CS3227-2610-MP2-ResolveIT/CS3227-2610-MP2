The restore ran exactly once and completed successfully.

I inspected the repository state and found two independently reviewable outcomes:

1. `feat(ticket-summary): Format ticket summaries with priority`

   Body:

   ```text
   Display ticket priority alongside the subject in ticket summaries and
   document the user-visible format.

   Refs: #42
   ```

   Paths:

   - `src/resolveit/ticket/TicketSummary.java`
   - `src/test/resolveit/ticket/TicketSummaryTest.java`
   - `docs/UserGuide.md`

   This groups the ticket-summary implementation, its direct test, and the
   user-facing documentation for Issue #42.

2. `chore(source): Add Java source hygiene check`

   Body:

   ```text
   Add a repository check that rejects tab characters in Java sources and
   document how developers run it.
   ```

   Paths:

   - `scripts/check-source.sh`
   - `docs/DeveloperGuide.md`

   This is separate repository tooling and its developer documentation, not
   part of the ticket-summary feature.

The following restored paths will remain uncommitted and untouched:

- `notes/todo.txt` — personal note
- `.env` — local environment file, already ignored
- `build/` — generated build output, already ignored
- `.idea/` — IDE state, already ignored

No `.gitignore` changes are needed. The existing ignored entries correctly
protect the local environment, build output, and IDE state.

Please approve this commit grouping. After approval, I’ll stage the explicit
paths, inspect each staged diff and run the relevant checks, then ask for the
required confirmation immediately before creating each local commit.