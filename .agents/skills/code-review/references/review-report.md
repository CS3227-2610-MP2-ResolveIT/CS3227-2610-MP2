# Review report

The reviewer writes or updates exactly `reviews/issue-<number>.md`.
`<number>` is the target issue number. This file is the canonical review and
the handoff for follow-up work; do not create a second report for the same
issue.

Every finding has a stable ID in the form `CR-<issue>-<number>` and a status.
Keep IDs across reruns. Valid statuses are `Open`, `Accepted`, `In progress`,
`Fixed`, `Rejected`, `Not reproducible`, and `Deferred`.

```markdown
# Code Review Report

## Status
- Target:
- Scope:
- Overall: PASS | FINDINGS | BLOCKED
- Independent human review: READY | NOT READY | BLOCKED

## Checks
| Area | Status | Command/evidence |
| --- | --- | --- |
| Compilation | | |
| Tests | | |
| Smoke/acceptance testing | | |
| Static analysis | | |
| Packaging/resources | | |
| Specification validation | | |
| Documentation consistency | | |

## Next actions
- Rerun write-code skill: YES | NO
- Rerun update-documentation skill: YES | NO | AFTER CODE FIX
- Rerun code-review skill: YES | NO | AFTER FOLLOW-UP WORK
- Reason:

## Agent handoff
- Follow-up required: YES | NO | BLOCKED
- Current implementation/review state:
- Next responsible skill or human:
- Priority and finding addressed:
- Exact files and line locations:
- Required change or investigation:
- Constraints and out-of-scope work:
- Verification commands:
- Manual checks:
- Success criteria:
- Documentation impact:
- Required next rerun:
- Blocker or decision needed:

## Findings
- `CR-<issue>-<number>` — [severity] Title — Status: Open | Accepted | In progress | Fixed | Rejected | Not reproducible | Deferred
- Category:
- Location:
- Requirement:
- Evidence:
- Expected / actual:
- Reproduction or inspection path:
- Impact:
- Likely cause / confidence:
- Fix direction and affected responsibility:
- Tests to add/update:
- Documentation impact:
- Verification after fixing:

## Positive observations
-
## Gaps and limitations
-

## Manual checks for user
-

## Conclusion
-
```
