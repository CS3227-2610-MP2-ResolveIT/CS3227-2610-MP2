# javadoc-writing evaluation

Evaluates the `javadoc-writing` skill: documenting a scoped set of Java public
APIs to the CS2103/SE-EDU standard, without changing behaviour, signatures, or
out-of-scope files.

**Clean-baseline design.** Fixtures and prompts carry *no* coding standard and
are worded like a developer's plain request ("add Javadoc to X"). The standard
lives only in the skill, so the no-skill baseline reflects genuine base-agent
behaviour and any difference is attributable to the skill. Scope (which files)
is legitimate task input and may appear in a prompt; documentation *rules* may
not. Fixtures are disposable copies shaped like real ResolveIT domain code
(ticket, auth). The private `expected.md` holds the standard for the judges.

## Test cases

| Case | Invocation | Tests | Deterministic focus | Semantic focus |
|---|---|---|---|---|
| `document-public-api` | Implicit | Positive: document undocumented ticket API | class+method headers, `@param`/`@throws` on `assertCanCreate`, signatures intact, ticket-scope-only | quality, getter restraint (`getDailyLimit`), no invented contracts |
| `respect-scope-boundaries` | Implicit (scope given) | Scope discipline | scoped class documented, sibling unchanged, signatures intact | in-scope quality, sibling left undocumented |
| `no-change-when-already-documented` | Implicit | No-change accuracy | churn within threshold, signatures intact, no other file changed | restraint, honest reporting |
| `negative-control-inline-comments` | Implicit | Trigger discrimination | inline `//` added, no `/**` block added, signature intact | did the inline-comment task, did not misfire into API docs |

Each `check.py` emits the shared grade shape (`overall_pass`, `score` = sum of
`checks[].points`, `checks[]`, `limitations`), diffs worker changes against the
prepared base (`HEAD`), and states its own limitations. The two active-
documentation cases carry a zero-point `skill-read-when-available` trace check
that confirms the injected skill was read in the with-skill configuration.

## Running

```bash
# Structural validation only — no model calls
python3 .agents/evals/eval.py check javadoc-writing

# Run all cases; --compare adds the no-skill baseline
python3 .agents/evals/eval.py run javadoc-writing --compare --judges 1
```

## Limitations

- Disposable fixtures have no Gradle build, so `javadoc` doclint is not run
  inside a trial; doclint remains the skill's real signal on the live project.
- Deterministic checks grade structure and scope; they do not detect
  over-documentation (for example a verbose getter header). That is left to the
  semantic judges.
- Trial counts are small and there is no holdout set; treat results as
  indicative, not a benchmark.

## Runs

`experiment-001..007` predate the clean-baseline redesign or were interrupted
smoke tests; they tested now-replaced fixtures or leaked the standard into the
fixture, so they are not evidence for the current suite.

### experiment-008 — clean baseline, `--compare --trials 3 --judges 1 --parallel`

Report: [runs/experiment-008/report.md](runs/experiment-008/report.md).
Outcome FAIL (18/24 trials passed under a single-judge bar), mean 92.67.

| Case | Baseline | With skill | Difference |
|---|---:|---:|---:|
| document-public-api | 63.67 | 88.33 | +24.66 |
| negative-control-inline-comments | 94.33 | 95.0 | +0.67 |
| no-change-when-already-documented | 100.0 | 100.0 | +0.0 |
| respect-scope-boundaries | 100.0 | 100.0 | +0.0 |

Reading:

- The skill's benefit is concentrated in `document-public-api` (+24.66). All
  three baseline trials over-documented the trivial `getDailyLimit` getter with
  a verbose `@return`; the with-skill runs applied getter restraint in two of
  three trials, so the skill improves but does not guarantee the behaviour.
- The skill is correctly neutral on `no-change`, `respect-scope-boundaries`, and
  the negative control, showing it does not cause regressions where base
  behaviour is already adequate.
- Caveats: single-judge verdicts (read scores, not pass/fail) and three trials
  per cell — treat small deltas as noise. One with-skill `document-public-api`
  trial scored 100 but shows as failing on the deterministic `signatures-intact`
  check because it reformatted the `Ticket` record onto multiple lines to attach
  component Javadoc; the signature is unchanged, so this is a grader
  exact-match limitation, not a worker signature change.
