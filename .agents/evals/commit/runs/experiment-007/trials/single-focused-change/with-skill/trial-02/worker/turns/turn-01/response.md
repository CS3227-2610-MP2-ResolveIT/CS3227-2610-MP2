The restored changes form one coherent Issue #63 outcome, so I propose one focused commit:

1. `feat(ticket): Show priority as coloured badge`

   Body:

   ```text
   Render HIGH, MEDIUM, and LOW ticket priorities with coloured emoji
   badges, and cover the HIGH badge with a regression test. Document the
   new badge format in the user guide.

   Refs: #63
   ```

   Paths:

   - `src/resolveit/ticket/PriorityBadge.java` — badge renderer
   - `src/test/resolveit/ticket/PriorityBadgeTest.java` — renderer test
   - `docs/UserGuide.md` — user-facing badge documentation

   These belong together because the implementation, its direct test, and the
   documentation all deliver the single requested feature.

No paths will be left uncommitted or excluded. The working tree contains no
unrelated changes, and `.gitignore` requires no modification.

Please approve this grouping so I can stage the explicit paths, run the relevant
checks, inspect the staged diff, and then ask for the separate confirmation
required immediately before creating the commit.