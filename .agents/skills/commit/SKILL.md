---
name: commit
description: Prepare and create local commits in focused semantic groups using Conventional Commits. Use when the user wants to commit or organize commits.
---

# Commit

Create focused, reviewable commits from existing changes.

- Inspect `.gitignore`, `git status`, staged and unstaged diffs, and untracked files.
- Preserve unrelated user changes.
- Reject staging secrets, IDE state, or unapproved generated output.
- Ask before editing `.gitignore` or committing.
- This skill handles local commit creation only; history rewriting, pushing, tagging, and issue-closing are outside its scope.

## Gitignore

Inspect `.gitignore` before grouping changes.

If it is missing or incomplete:

1. identify only clearly generated, local, IDE, build, or secret files that should be ignored;
2. show the proposed entries;
3. ask for approval;
4. create or update `.gitignore`;
5. treat the change as a separate `chore` commit.

Never use `.gitignore` to hide an uncertain or user-owned file.

## Commits

Group changes by intent, not merely by file. Each commit should represent one independently valid logical outcome. Keep inseparable implementation, tests, and documentation together.

### Commit grouping standard

There is no fixed one-issue-to-one-commit rule. Use the following decision
standard for issue work:

1. Start by identifying the independently meaningful outcomes in the issue.
   A small issue may fit naturally in one feature commit, while a larger issue
   may need several commits. Keep implementation, its tests, and the
   documentation that describes the delivered behaviour together when they
   form one independently valid outcome.
2. Create a separate commit for unrelated tooling, process, or repository
   maintenance work, even if it was done during the same session. Examples
   include static-analysis configuration, agent-skill changes, CI changes, and
   `.gitignore` updates.
3. Use multiple commits for one issue when each commit has a clear,
   reviewable purpose and is independently valid or is a necessary,
   understandable step in the sequence. Examples include a preparatory
   refactor followed by the feature, separate domain and UI layers, or a
   separately releasable follow-up. Do not split merely because the
   implementation took multiple iterations.
4. Keep review fixes in the original feature commit when the feature has not
   yet been committed. If the feature was already committed and a later review
   reopens it, use a focused follow-up `fix` or `refactor` commit and explain
   the relationship in its body.
5. An issue, log, or development session may cover one commit or several
   commits. There is no need to force all related work into one commit or to
   create multiple commits artificially. Every commit must have one clear
   intent, and the commit sequence should be easy to review and explain.
   Commit boundaries follow repository outcomes, not prompts, tool invocations,
   or the number of files changed.
6. Include each development log with the commit for the outcome it records.
   A feature implementation log belongs with the feature commit; a separate
   tooling or process log belongs with that maintenance commit. If one log
   genuinely covers several commits, place it with the primary outcome and
   explain the related commits in their bodies when useful. Do not duplicate
   logs across commits.

Before staging, classify every changed path as either part of the primary
issue outcome or a separate maintenance outcome. If both kinds are present,
prepare separate commits with explicit paths. When an issue has multiple
outcomes, divide it into the smallest sensible sequence of commits and state
the relationship in the commit bodies where useful. When uncertain, prefer
the smallest set of commits that each builds, tests, and communicates one
coherent change without separating inseparable feature code from its tests or
user and developer documentation.

### Whole commit plan

Before staging any group, show the complete proposed commit sequence at once.
For every planned commit, state its order, Conventional Commit message and
body (including any `Refs: #<number>` trailer), classified paths, and why it
is a separate outcome. Also state every path intentionally left uncommitted or
excluded, including an ignored file that would need explicit inclusion. Wait
for the user to approve or adjust this grouping before staging. This plan
approval does not replace the explicit confirmation required immediately
before each `git commit`.

Stage explicit paths only:

```bash
git add -- path/to/file
```

Use Conventional Commits:

```text
<type>[optional scope][!]: <description>
```

- `feat` adds a feature.
- `fix` fixes a bug.
- Use `test`, `docs`, `refactor`, or `chore` for those respective changes.
- Use `!` or a `BREAKING CHANGE:` footer for breaking changes.
- Keep the description concise and specific.
- Use a scope when helpful. Write the description in the imperative mood,
  capitalize it, omit trailing punctuation, aim for 50 characters, and never
  exceed 72.
- For non-trivial commits, add a body explaining what changed and why, wrapped
  at 72 characters.
- When a commit delivers work for a known GitHub issue, add a separate final
  body line `Refs: #<number>`. Use the issue identified by the request, branch,
  or reviewed change. This records traceability without closing an issue;
  closing keywords such as `Fixes` or `Closes` remain for the separate
  close-issue workflow. Do not invent an issue reference for unrelated
  maintenance work or when the issue number is unknown.

Before each commit:

- inspect the staged diff;
- run relevant checks;
- show the proposed message, staged paths, and remaining changes;
- ask for explicit confirmation immediately before `git commit`.

After approval, create only the approved commit and report its hash. Leave pushing to a separate request.
