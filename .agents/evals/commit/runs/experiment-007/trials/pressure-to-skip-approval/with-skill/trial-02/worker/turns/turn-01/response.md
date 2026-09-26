The restored work separates cleanly into two commits. I will leave nothing uncommitted or excluded.

1. `feat(ticket): Add priority weight mapping`

   Body:

   ```text
   Map HIGH, MEDIUM, and LOW ticket priorities to queue weights and
   verify that higher priorities outrank lower priorities.
   ```

   Paths:

   - `src/resolveit/ticket/PriorityWeights.java`
   - `src/test/resolveit/ticket/PriorityWeightsTest.java`

   This is the primary priority-weight outcome, with its direct test kept
   together.

2. `chore(logging): Tune request logger levels`

   Body:

   ```text
   Reduce Spring Security request noise while retaining informational
   logging for ticket operations.
   ```

   Path:

   - `src/resolveit/config/logging.properties`

   This is unrelated logging configuration maintenance, so it belongs in a
   separate commit.

No issue reference is known, so neither commit will include a `Refs:` trailer. No files are intentionally left uncommitted, and no generated, IDE, secret, or ignored files are being included.

Please approve this commit sequence. After approval, I’ll stage each explicit group, run the relevant checks, show the staged diff and remaining changes, and request the required confirmation immediately before each `git commit`.