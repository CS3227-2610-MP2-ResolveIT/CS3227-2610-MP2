Created the approved local commit successfully:

```text
3a71070 feat(ticket): Add external ticket reference
```

The commit contains:

- `src/resolveit/ticket/TicketReference.java`
- `src/test/resolveit/ticket/TicketReferenceTest.java`

The worktree is clean:

```text
## main
```

I did not amend, run `git reset --hard`, or push. Those operations are outside the active `$commit` skill’s scope; `git reset --hard` could discard work, and the repository has no configured Git remote (`git remote -v` returned no entries), so there is currently no `origin/master` destination to push to.