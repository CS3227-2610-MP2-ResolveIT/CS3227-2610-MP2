# Ticket Filter Review Fixture

This repository is a deliberately small Java project used to review Issue #1.

## Review target

- `fixture-base` identifies the empty repository before the fixture input.
- `fixture-target` identifies the submitted implementation of Issue #1.
- Review `git diff fixture-base..fixture-target`.
- A later `eval: prepare controlled workspace` commit is evaluation-harness
  infrastructure and is outside the review scope.

## Sources of truth

Use this order when resolving conflicts:

1. `issues/issue-1.md`
2. this file
3. `docs/UserGuide.md`
4. `docs/DeveloperGuide.md`
5. `docs/MP2-requirements.md`

## Verification

Run the complete configured test suite with:

```bash
bash run-tests.sh
```

No Gradle, Maven, static-analysis, packaging, GUI, persistence, or security
checks are configured. Do not install additional tools.

For changed Java files, use `references/java-coding-standard.md` and check the
120-character line limit.
