The approved changes are staged and the feature check passed.

Proposed commit:

```text
feat(ticket): Add external ticket reference

Add a stable reference code using the ticket category prefix and
zero-padded numeric ID.

Include a focused executable test for hardware ticket references.

Refs: #71
```

Staged paths:

- `src/resolveit/ticket/TicketReference.java`
- `src/test/resolveit/ticket/TicketReferenceTest.java`

Verification:

- `bash run-tests.sh` passed
- `git diff --cached --check` passed
- Staged diff contains 19 inserted lines across the two intended files
- No remaining unstaged or untracked work is present

Please provide explicit confirmation to run `git commit` with this message.