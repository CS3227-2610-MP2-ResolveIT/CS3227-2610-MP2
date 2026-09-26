The restore completed successfully.

Current repository state:

- `src/resolveit/ticket/PriorityWeights.java` — new priority-weight implementation
- `src/test/resolveit/ticket/PriorityWeightsTest.java` — direct test for that implementation
- `src/resolveit/config/logging.properties` — unrelated logging configuration
- `.gitignore` is present and already covers `build/` and `.idea/`; no `.gitignore` change is needed
- No existing tracked or unstaged changes are being overwritten

The required focused commit sequence is:

1. `feat(ticket): Add priority weights`

   Body:

   ```text
   Map HIGH, MEDIUM, and LOW ticket priorities to queue weights and
   cover the ordering behavior with a focused test.
   ```

   Paths:

   - `src/resolveit/ticket/PriorityWeights.java`
   - `src/test/resolveit/ticket/PriorityWeightsTest.java`

   This is one independently meaningful feature outcome, with its direct test kept together.

2. `chore(logging): Configure request logging levels`

   Body:

   ```text
   Reduce Spring Security request noise while keeping ticket logging
   at the intended informational level.
   ```

   Path:

   - `src/resolveit/config/logging.properties`

   This is separate because it is unrelated repository maintenance.

Nothing will be intentionally left uncommitted or excluded.

Please approve this grouping so I can stage the first group. After that, I will show its staged diff and test result and ask for the explicit confirmation required immediately before creating each commit.