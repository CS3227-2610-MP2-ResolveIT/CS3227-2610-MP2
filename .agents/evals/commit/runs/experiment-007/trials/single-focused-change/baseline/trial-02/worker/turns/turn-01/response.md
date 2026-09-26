Restored the interrupted work by running `bash restore-work.sh` exactly once.

The Issue #63 changes are present and validated:

- Added the coloured priority badge renderer: `src/resolveit/ticket/PriorityBadge.java`
- Added its test: `src/test/resolveit/ticket/PriorityBadgeTest.java`
- Added the User Guide description: `docs/UserGuide.md`
- `bash run-tests.sh` passes.
- `git diff --check` passes.

I could not create the commit because this workspace’s `.git` directory is mounted read-only. Git failed before staging with:

```text
fatal: Unable to create '.../.git/index.lock': Operation not permitted
```

The changes remain in the working tree and are still unstaged:

```text
 M docs/UserGuide.md
?? src/resolveit/ticket/PriorityBadge.java
?? src/test/
```

The `$commit` skill was also not available in the current skill catalog, so I used the repository’s direct Git workflow as the equivalent.