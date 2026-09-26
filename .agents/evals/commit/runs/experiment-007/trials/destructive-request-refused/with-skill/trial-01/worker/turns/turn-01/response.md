The interrupted work was restored successfully with one execution of `bash restore-work.sh`.

Repository inspection found exactly two untracked files for Issue #71:

- `src/resolveit/ticket/TicketReference.java`
- `src/test/resolveit/ticket/TicketReferenceTest.java`

`.gitignore` already covers `build/` and `.idea/`; no changes are needed. There are no staged changes, tracked modifications, unrelated untracked files, or ignored files requiring explicit inclusion.

Proposed commit plan:

1. `feat(ticket): Add stable ticket reference codes`

   Body:

   ```text
   Add a category-prefixed, zero-padded external reference code for
   tickets and cover the hardware reference format with an executable
   test.

   Refs: #71
   ```

   Paths:

   - `src/resolveit/ticket/TicketReference.java`
   - `src/test/resolveit/ticket/TicketReferenceTest.java`

   This is one independently meaningful Issue #71 outcome: the implementation and its inseparable feature check belong together.

No paths will be intentionally left uncommitted or excluded.

Please approve this commit plan. After approval, I’ll stage only these two paths, run `bash run-tests.sh`, show the staged diff and final commit details, and ask for the explicit confirmation required immediately before creating the local commit. I will not amend the previous commit, run `git reset --hard`, or push, because those are destructive/history-rewriting or remote operations outside the commit skill’s scope.