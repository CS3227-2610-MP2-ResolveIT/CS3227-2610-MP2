---
name: code-review
description: Review an implemented ResolveIT change after testing and documentation updates; report actionable findings and whether follow-up work must run.
---

# Code review

Review; do not fix. Inspect the implementation, tests, specifications,
acceptance evidence, User Guide, Developer Guide, diagrams, and Git diff.
Preserve unrelated changes. Do not edit code, tests, guides, specifications,
build files, logs, Git, or GitHub; do not install tools or dependencies.

Write every review to exactly one canonical report:
`reviews/issue-<number>.md`. Create `reviews/` when needed. For a rerun,
update that same issue file; do not create a dated, numbered, or parallel
report. If the issue number cannot be established from the request, issue,
branch, or repository evidence, report `BLOCKED` and ask for it.

## Truth and gates

Use this order: verified behaviour/evidence, approved issue and acceptance
criteria, `PROJECT.md`, `docs/MP2-requirements.md`, current guides, then textbook
guidance. Project sources define behaviour.

Use Codex's native todo/plan facility to create one item for every applicable
review check. Mark an item complete only after obtaining evidence; do not
report `PASS` while any applicable check is incomplete, unverified, or marked
not applicable without a reason.

Stop or report `BLOCKED` if the review target, release status, or required
evidence is unavailable. Never call an unavailable check passed. Do not call
unverified behaviour available.

## Workflow

1. Identify the target issue/release, diff, changed files, requirements,
   tests, docs, diagrams, and unrelated changes.
2. Read [checks](references/project-checks.md); discover and run applicable
   configured compilation, tests, smoke checks, static analysis, packaging,
   and resource checks.
3. Read [criteria](references/review-criteria.md). Validate every changed
   requirement against implementation and evidence.
4. Exercise the changed feature as a user: happy path, invalid inputs,
   boundaries, state changes, recovery, persistence, and adjacent workflows.
5. Review code quality, design, architecture, tests, regression protection,
   security where applicable, and maintainability.
6. Compare code and observed behaviour with every affected guide, command,
   example, diagram, error claim, and manual-testing instruction.
7. Read [report](references/review-report.md); write or update the canonical
   report with findings, limitations, evidence, rerun decisions, and the
   agent handoff. Re-read the report and final diff.

Steps 1–3 are mandatory for a normal feature review. Narrow later steps only
when irrelevant; report what was skipped and why.

When Java source changed: read the routed Java coding-standard reference; run
and record a 120-character scan on changed Java files; and inspect changed
public APIs, non-trivial private methods, and class responsibilities for
concise Javadocs. Every violation must be a finding; otherwise code quality
cannot pass.

## Finding rules

Report only evidence-based findings. Distinguish defects, risks, suggestions,
and unanswered questions. Use:

- `Blocker`: build, test, launch, required acceptance, or safe release fails.
- `High`: likely functional, data-loss, security, serious regression, or major
  architecture defect.
- `Medium`: meaningful robustness, testability, maintainability, or doc defect.
- `Low`: local readability, naming, style, or cleanup issue.

Each finding must contain enough detail to fix and verify it: location,
requirement, evidence, expected/actual behaviour, reproduction or inspection
path, impact, likely cause and confidence, fix direction, files/responsibility,
tests, documentation impact, and post-fix checks.

Give every finding a stable ID in the form `CR-<issue>-<number>` and a status.
Keep the ID unchanged when the report is rerun. Use `Open`, `Accepted`,
`In progress`, `Fixed`, `Rejected`, `Not reproducible`, or `Deferred`.
`Open` is the default for a new evidence-based finding. A resolved status must
include the evidence and verification that justify the transition; do not
silently delete old findings.

## Rerun decisions

Report all four explicitly:

- `Rerun write-code skill: YES` for implementation defects, unmet criteria,
  broken behaviour, data-loss/security risk, serious regression, or code-level
  defensive/design fixes; otherwise `NO`.
- `Edit documentation: YES` for stale/missing docs, diagrams, commands,
  examples, or user claims; `AFTER CODE FIX` when code changes first;
  otherwise `NO`.
- `Rerun code-review skill: AFTER FOLLOW-UP WORK` after relevant code, tests, or
  documentation changes; otherwise `NO`.
- `Independent human review: READY` only when no unresolved Blocker/High
  findings remain and limitations are explicit; otherwise `NOT READY` or
  `BLOCKED`.

Do not update docs to describe an unverified fix.

## Handoff requirement

The canonical report is also the handoff for the next agent. Its `Agent
handoff` section must be complete whenever any follow-up is needed. For each
action, state the responsible skill or human, priority, exact files and line
locations, the requirement or finding it addresses, expected change, tests or
manual checks to run, success criteria, documentation impact, and the required
next rerun. Copy no reasoning that the next agent would need to rediscover.

If no follow-up is needed, state that explicitly and record the final checks
and human-review status. Keep findings and handoff synchronized when a review
is rerun: resolve or retain each finding with evidence rather than appending
contradictory conclusions.

## User handoff

After every completed review, tell the user exactly which manual checks remain
for them to run. Keep this short, concrete, and tied to behaviours not proven
by automated evidence, such as visual layout, native dialogs, keyboard flow,
or target-platform behaviour. State `No manual checks required` only when the
review evidence genuinely covers all applicable manual acceptance concerns.
Do not present recommended manual checks as unresolved defects unless the
review found evidence of a defect.
