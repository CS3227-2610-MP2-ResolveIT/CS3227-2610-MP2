# Javadoc-writing skill evaluation and eval-harness parallelisation

Date: 2026-09-25
Time: 11:30:00 SGT (session start, filename precision)
Timestamp source: Conversation wall-clock on 2026-09-25; not derived from a
Codex session id.
Task: Build and run a clean-baseline evaluation of the javadoc-writing skill,
correct grader/design flaws found during runs, and add a --parallel mode to the
eval harness.
Status: completed
Interaction range: Eval-suite design from ~11:30 SGT through the experiment-008
analysis at ~16:47 SGT on 2026-09-25.
Evidence sources: This conversation; `.agents/evals/javadoc-writing/` fixtures,
prompts, `check.py`, `expected.md`, and `README.md`; experiment run reports and
per-trial `result.json`/trace under `.agents/evals/javadoc-writing/runs/`;
`.agents/evals/eval.py` and `_lib/harness.py`; upstream repo
`github.com/junhao4/codex-skill-evals` commits `1810a58`, `24d5dcf`, `65edb62`.
Related logs: 2026-09-25-093700-javadoc-skill-authoring.md, summary-1.md
Human verification: approved

## Interaction history

### 1. Build the four-case evaluation suite

- Faithful paraphrase: build a rigorous L4/L5 suite (positive, scope-boundary,
  no-change, negative-control) with deterministic and semantic graders.
- AI response: created four disposable fixtures, prompts, `check.py`, and
  `expected.md`, plus a suite README; validated structurally with no model
  calls.
- User correction or decision: requested a broad multi-case suite rather than a
  single focused case.
- Resulting action: four test cases structurally validated.

### 2. Confirm skill isolation

- Faithful paraphrase: run the suite and check whether `--compare` isolates the
  skill.
- AI response: confirmed skill isolation works — the baseline workspace had no
  skill file and the with-skill workspace had it injected.
- User correction or decision: none required.
- Resulting action: proceeded to a clean-baseline redesign.

### 3. Redesign for a true baseline

- Faithful paraphrase: the baseline is not a real base-agent baseline because
  the prompt and PROJECT.md carried the coding standard; nuke and rewrite.
- AI response: agreed the fixtures leaked the standard into the baseline;
  rewrote all four cases so prompts are plain requests and fixtures carry no
  standard, keeping the standard only in the skill and the private `expected.md`.
  Regrounded fixtures on real ResolveIT domain code (ticket, auth).
- User correction or decision: required plain prompts, no standard leakage, and
  fixtures related to the actual repository.
- Resulting action: four cases rebuilt and structurally revalidated.

### 4. Add a --parallel mode to the harness

- Faithful paraphrase: runs are slow; batch the semantic judges and parallelise
  them, keeping the current harness intact; do it in the upstream harness repo.
- AI response: added an opt-in `--parallel` flag that runs all workers plus
  deterministic grading first, then batches the semantic judges in parallel,
  then writes the report; capped the batch at eight concurrent trials; grouped
  the output into phases and made the deferred-semantic message accurate. The
  default path is unchanged.
- User correction or decision: keep changes in the upstream repo, minimise
  unnecessary code, cap concurrency at eight, and improve the phase output.
- Resulting action: committed and pushed upstream (`1810a58`, `24d5dcf`,
  `65edb62`); synced into the MP2 vendored copy, left uncommitted in MP2.

### 5. Full clean-baseline run and analysis

- Faithful paraphrase: run the full 24-trial suite and analyse.
- AI response: ran `--compare --trials 3 --judges 1 --parallel` (experiment-008)
  and analysed the report and per-trial results.
- User correction or decision: chose three trials with one judge and the
  concurrency cap.
- Resulting action: recorded the results in the suite README's Runs section.

## Work and verification

- Proposed, approved, and executed: built the eval suite, rewrote it for a clean
  baseline, added `--parallel` upstream, ran the full suite, and documented the
  results.
- Files changed: added `.agents/evals/javadoc-writing/` (fixtures, prompts,
  graders, README); modified vendored `.agents/evals/eval.py` and
  `_lib/harness.py` (synced from upstream, uncommitted in MP2). Upstream repo
  commits `1810a58`, `24d5dcf`, `65edb62` pushed.
- Checks and observed results: `eval.py check javadoc-writing` structurally
  valid after every change; graders compiled and dry-ran correctly; the
  `--parallel` path produced correct merged `result.json` files matching the
  sequential path.
- Errors, limitations, or remaining uncertainty: single-judge verdicts and
  three trials per cell make small deltas noise. Disposable fixtures have no
  Gradle build, so `javadoc` doclint is not exercised inside a trial.

## Evaluation evidence

- Skill and fixture: `javadoc-writing`; four disposable ResolveIT-domain cases
  under `.agents/evals/javadoc-writing/test-cases/`.

- What each semantic judge scored (private `expected.md` rubrics; the worker
  never sees these — L4 model-assisted grading against a fixed rubric schema):
  - document-public-api: Documentation quality (WHAT/WHY, correct tags, no
    invented contracts) — 40; Getter restraint (omit or one-line the trivial
    `getDailyLimit`, no verbose `@return`) — 35; Standard-conformant structure
    and scope — 25.
  - respect-scope-boundaries: In-scope documentation quality — 55; Scope
    discipline (leave the undocumented sibling untouched) — 45.
  - no-change-when-already-documented: Restraint (no churn of already-correct
    Javadoc) — 50; Honest reporting (state docs were already adequate) — 50.
  - negative-control-inline-comments: Correct task discrimination (do not
    misfire into Javadoc) — 55; Inline-comment quality — 45.

- Deterministic checks (separate axis, L4/L5 grader taxonomy): per-member
  Javadoc presence, `@param`/`@throws`, signatures unchanged, out-of-scope
  package unchanged (diffed against `HEAD`), and a zero-point trace check that
  the skill was read in the with-skill configuration.

- Grader results (experiment-008, `runs/experiment-008/report.md`): mean 92.67,
  18/24 trials passed under one judge. Baseline -> with-skill means —
  document-public-api 63.67 -> 88.33 (+24.66); negative-control 94.33 -> 95.0;
  no-change 100 -> 100; respect-scope-boundaries 100 -> 100.

- Controlled comparison / isolation: `--compare` verified from the workspace —
  no skill file in baseline, present in with-skill; the `skill-read` trace check
  passed where applicable.

- Agent vs human verification: scores are the harness's deterministic checks
  plus one semantic judge per trial; the human ran the harness and reviewed the
  report and per-trial evidence, and did not re-grade individual judge scores.

- Limitations: one judge per trial (no majority vote); three trials per cell; no
  holdout set; no live Gradle doclint in-fixture.

## Reflection notes

- What the evaluation tested, in lecture terms: the suite turns the skill's
  rules into observable requirements (L5 workflow contract). Deterministic
  checks and a semantic jury are kept as separate grader kinds (L4/L5), so a
  trial's structural correctness and its documentation quality are measured
  independently. The four cases span the L5 task-dataset shape: a feature-style
  positive case, a scope/protected-boundary case, a should-result-in-no-change
  case, and a negative control that should not trigger the skill.

- Main finding — the skill's value is restraint, and only the semantic axis sees
  it. The deterministic grader scored the positive case's with-skill runs as
  fully documented (structure intact), but the semantic judges revealed the real
  difference: all three baseline trials over-documented the trivial
  `getDailyLimit` getter with a verbose `@return`, while two of three with-skill
  trials applied the getter-omission rule (baseline 63.67 -> with-skill 88.33,
  +24.66). The guardrail being tested — comment minimally, omit trivial getters,
  WHAT/WHY not HOW — is not mechanically checkable, which is why the semantic
  axis is necessary.

- Guardrails and no-regression evidence. The skill was correctly neutral on the
  cases where base behaviour is already adequate: no-change accuracy
  (100 = 100, no churn of correct Javadoc), scope discipline (100 = 100, the
  sibling left untouched), and the negative control (95 vs 94, no Javadoc
  misfire). Neutral here is the desired result — it shows the skill enforces its
  guardrails without causing regressions or over-triggering, the L5
  no-change-accuracy / scope-accuracy / prohibited-action concern.

- Reliability and honest limits. The skill improves but does not guarantee the
  behaviour: one of three with-skill positive trials still over-documented the
  getter, so the guardrail is probabilistic rather than enforced. A plausible
  mechanism, visible in earlier traces, is doclint warning-aversion — the model
  documents the getter to silence a warning, against the skill's instruction to
  report warnings honestly rather than over-document.

- Human judgement required. The human caught the leaked-baseline design flaw and
  required the rewrite, grounded fixtures in real repository code, set the
  parallel concurrency cap, and constrained scope to necessary code — decisions
  the agent would not have made unprompted.

- What to do differently next time: use more trials or a majority-judge jury for
  firmer verdicts, and exercise real `javadoc` doclint where a build is
  available.
