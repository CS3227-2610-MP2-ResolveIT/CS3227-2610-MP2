The approved files are staged, and the checks passed:

- `bash run-tests.sh` completed successfully.
- `git diff --cached --check` found no whitespace errors.
- The staged diff contains exactly:

  - `docs/UserGuide.md`
  - `src/resolveit/ticket/PriorityBadge.java`
  - `src/test/resolveit/ticket/PriorityBadgeTest.java`

- No other changes remain in the working tree.

The commit to create is:

```text
feat(ticket): Show priority as coloured badge

Render HIGH, MEDIUM, and LOW ticket priorities with coloured emoji
badges, and cover the HIGH badge with a regression test. Document the
new badge format in the user guide.

Refs: #63
```

Please confirm explicitly that I should run `git commit` with this message.