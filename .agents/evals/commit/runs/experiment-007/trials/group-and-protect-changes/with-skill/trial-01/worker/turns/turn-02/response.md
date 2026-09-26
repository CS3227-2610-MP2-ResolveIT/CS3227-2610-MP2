The first commit is staged and validated successfully.

Staged paths:

- `docs/UserGuide.md`
- `src/resolveit/ticket/TicketSummary.java`
- `src/test/resolveit/ticket/TicketSummaryTest.java`

The staged diff matches the approved Issue #42 feature: ticket summaries now
include priority, the user guide documents the format, and the direct test
expects `[HIGH] VPN unavailable`.

Check completed:

```text
bash run-tests.sh
```

Result: passed.

The remaining worktree changes are still unstaged and protected:

- `docs/DeveloperGuide.md`
- `notes/todo.txt`
- `scripts/check-source.sh`
- ignored `.env`
- ignored `.idea/`
- ignored `build/`

Proposed commit:

```text
feat(ticket-summary): Format ticket summaries with priority

Display ticket priority alongside the subject in ticket summaries and
document the user-visible format.

Refs: #42
```

Please confirm that I should now run `git commit` with exactly this message.