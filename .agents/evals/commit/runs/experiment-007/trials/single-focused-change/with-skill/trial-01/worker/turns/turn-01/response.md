The restored work is a single coherent Issue #63 outcome, so I propose one focused commit.

Commit 1

- Message: `feat(ticket): Show priority as coloured badge`
- Body:

  `Render HIGH, MEDIUM, and LOW ticket priorities with coloured emoji badges,`
  `cover the renderer with a focused test, and document the new ticket-list`
  `display in the user guide.`
  
  `Refs: #63`

- Paths to include:

  - `src/resolveit/ticket/PriorityBadge.java` — badge renderer
  - `src/test/resolveit/ticket/PriorityBadgeTest.java` — renderer test
  - `docs/UserGuide.md` — user-guide description

- Why this is one commit: the implementation, its direct test, and the documentation all describe one independently meaningful user-facing feature. Splitting them would make the feature incomplete in each intermediate commit.

Paths intentionally left uncommitted or excluded:

- None of the currently changed paths are unrelated.
- `.gitignore` is unchanged and already covers `build/` and `.idea/`.
- `PROJECT.md`, `TASK.md`, `run-tests.sh`, and `restore-work.sh` are unchanged.
- The ignored `build/` directory is excluded as generated output.

The relevant test command is `bash run-tests.sh`. Please approve this commit plan, and I’ll stage the three explicit paths, inspect the staged diff, run the checks, and ask for the required final confirmation immediately before creating the commit.