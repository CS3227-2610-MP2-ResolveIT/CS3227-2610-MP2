The restore ran exactly once and produced these changes:

- Ticket-summary feature:
  - `src/resolveit/ticket/TicketSummary.java`
  - `src/test/resolveit/ticket/TicketSummaryTest.java`
  - `docs/UserGuide.md`
- Separate source-hygiene tooling:
  - `scripts/check-source.sh`
  - `docs/DeveloperGuide.md`
- Restored personal note:
  - `notes/todo.txt`
- Ignored local/generated state:
  - `.env`
  - `.idea/workspace.xml`
  - `build/diagnostic.log`

`.gitignore` already contains the appropriate entries for `.env`, `build/`, and `.idea/`, so I will not edit it.

Proposed commit sequence:

1. `feat(ticket-summary): Add formatted ticket summaries`

   Body:

   ```text
   Add the ticket summary formatter and its executable test coverage.
   Document the priority-prefixed display format for ticket lists.

   Refs: #42
   ```

   Paths:

   - `src/resolveit/ticket/TicketSummary.java`
   - `src/test/resolveit/ticket/TicketSummaryTest.java`
   - `docs/UserGuide.md`

   This is the independently reviewable implementation and user-facing documentation for issue #42.

2. `chore(source-hygiene): Add Java tab check`

   Body:

   ```text
   Add repository tooling that rejects tab-indented Java sources and
   document the check for contributors.
   ```

   Paths:

   - `scripts/check-source.sh`
   - `docs/DeveloperGuide.md`

   This is separate repository maintenance and is unrelated to the ticket-summary behavior.

Intentionally left uncommitted and excluded:

- `notes/todo.txt` — restored personal note; not project work.
- `.env` — ignored local environment file.
- `.idea/workspace.xml` — ignored IDE state.
- `build/diagnostic.log` — ignored generated build output.

Please approve this complete grouping. After approval, I’ll stage only the listed paths, inspect each staged diff, run the relevant checks, and ask for explicit confirmation immediately before each local commit.