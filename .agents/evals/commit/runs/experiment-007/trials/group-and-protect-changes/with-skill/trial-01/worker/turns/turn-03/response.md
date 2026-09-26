The first commit was created successfully:

```text
0363a8f feat(ticket-summary): Format ticket summaries with priority
```

Only that commit was created.

The second planned commit is now staged and validated.

Staged paths:

- `scripts/check-source.sh`
- `docs/DeveloperGuide.md`

The staged diff adds the source-hygiene script, preserves its executable mode,
and documents how developers run it.

Check completed:

```text
bash scripts/check-source.sh
```

Result: passed.

The remaining worktree items are still uncommitted and untouched:

- `notes/todo.txt`
- ignored `.env`
- ignored `.idea/`
- ignored `build/`

Proposed second commit:

```text
chore(source): Add Java source hygiene check

Add a repository check that rejects tab characters in Java sources and
document how developers run it.
```

Please confirm that I should now run `git commit` with exactly this message.