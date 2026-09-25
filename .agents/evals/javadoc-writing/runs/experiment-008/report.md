# Evaluation report: javadoc-writing

## Result

| Field | Value |
|---|---|
| Outcome | **FAIL** |
| Mean score | 92.67/100 |
| Passed trials | 18/24 |
| Completed trials | 24/24 |
| Experiment status | completed |
| Semantic judges | 1 per eligible trial |

## Trial results

| Test case | Configuration | Trial | Score | Pass | Details |
|---|---|---:|---:|---:|---|
| document-public-api | baseline | 1 | 65 | No | [result.json](trials/document-public-api/baseline/trial-01/result.json) |
| document-public-api | baseline | 2 | 65 | No | [result.json](trials/document-public-api/baseline/trial-02/result.json) |
| document-public-api | baseline | 3 | 61 | No | [result.json](trials/document-public-api/baseline/trial-03/result.json) |
| document-public-api | with-skill | 1 | 65 | No | [result.json](trials/document-public-api/with-skill/trial-01/result.json) |
| document-public-api | with-skill | 2 | 100 | No | [result.json](trials/document-public-api/with-skill/trial-02/result.json) |
| document-public-api | with-skill | 3 | 100 | Yes | [result.json](trials/document-public-api/with-skill/trial-03/result.json) |
| negative-control-inline-comments | baseline | 1 | 100 | Yes | [result.json](trials/negative-control-inline-comments/baseline/trial-01/result.json) |
| negative-control-inline-comments | baseline | 2 | 93 | Yes | [result.json](trials/negative-control-inline-comments/baseline/trial-02/result.json) |
| negative-control-inline-comments | baseline | 3 | 90 | Yes | [result.json](trials/negative-control-inline-comments/baseline/trial-03/result.json) |
| negative-control-inline-comments | with-skill | 1 | 95 | Yes | [result.json](trials/negative-control-inline-comments/with-skill/trial-01/result.json) |
| negative-control-inline-comments | with-skill | 2 | 100 | Yes | [result.json](trials/negative-control-inline-comments/with-skill/trial-02/result.json) |
| negative-control-inline-comments | with-skill | 3 | 90 | No | [result.json](trials/negative-control-inline-comments/with-skill/trial-03/result.json) |
| no-change-when-already-documented | baseline | 1 | 100 | Yes | [result.json](trials/no-change-when-already-documented/baseline/trial-01/result.json) |
| no-change-when-already-documented | baseline | 2 | 100 | Yes | [result.json](trials/no-change-when-already-documented/baseline/trial-02/result.json) |
| no-change-when-already-documented | baseline | 3 | 100 | Yes | [result.json](trials/no-change-when-already-documented/baseline/trial-03/result.json) |
| no-change-when-already-documented | with-skill | 1 | 100 | Yes | [result.json](trials/no-change-when-already-documented/with-skill/trial-01/result.json) |
| no-change-when-already-documented | with-skill | 2 | 100 | Yes | [result.json](trials/no-change-when-already-documented/with-skill/trial-02/result.json) |
| no-change-when-already-documented | with-skill | 3 | 100 | Yes | [result.json](trials/no-change-when-already-documented/with-skill/trial-03/result.json) |
| respect-scope-boundaries | baseline | 1 | 100 | Yes | [result.json](trials/respect-scope-boundaries/baseline/trial-01/result.json) |
| respect-scope-boundaries | baseline | 2 | 100 | Yes | [result.json](trials/respect-scope-boundaries/baseline/trial-02/result.json) |
| respect-scope-boundaries | baseline | 3 | 100 | Yes | [result.json](trials/respect-scope-boundaries/baseline/trial-03/result.json) |
| respect-scope-boundaries | with-skill | 1 | 100 | Yes | [result.json](trials/respect-scope-boundaries/with-skill/trial-01/result.json) |
| respect-scope-boundaries | with-skill | 2 | 100 | Yes | [result.json](trials/respect-scope-boundaries/with-skill/trial-02/result.json) |
| respect-scope-boundaries | with-skill | 3 | 100 | Yes | [result.json](trials/respect-scope-boundaries/with-skill/trial-03/result.json) |

## Baseline comparison

| Test case | Baseline | With skill | Difference |
|---|---:|---:|---:|
| document-public-api | 63.67 | 88.33 | +24.66 |
| negative-control-inline-comments | 94.33 | 95.0 | +0.67 |
| no-change-when-already-documented | 100.0 | 100.0 | +0.0 |
| respect-scope-boundaries | 100.0 | 100.0 | +0.0 |

## Main failure reasons

- **document-public-api / baseline / trial-01:** semantic/semantic-jury: 0/1 judges voted pass; valid judge scores: [65]; consensus uses the median.
- **document-public-api / baseline / trial-01:** semantic/getter-restraint: The trivial getDailyLimit getter is over-documented relative to the required restraint. It has a multi-line header and a verbose @return block, although the standard permits omitting it or using only a one-line form. This fails the full 35-point getter-restraint criterion.
- **document-public-api / baseline / trial-02:** semantic/semantic-jury: 0/1 judges voted pass; valid judge scores: [65]; consensus uses the median.
- **document-public-api / baseline / trial-02:** semantic/getter-restraint: The getDailyLimit getter is trivial, but it was given a multi-line Javadoc block with a verbose @return description. The expected standard permits omitting this getter's documentation or using only a one-line form; this is specifically the over-documentation that loses the criterion.
- **document-public-api / baseline / trial-03:** semantic/semantic-jury: 0/1 judges voted pass; valid judge scores: [61]; consensus uses the median.
- **document-public-api / baseline / trial-03:** semantic/documentation-quality: Most documentation is accurate, concise, and uses present-tense summaries. `remaining` correctly explains quota-counted tickets and its non-negative return, while `assertCanCreate` documents its parameter and the `QuotaExceededException` condition. However, the `Ticket` record's `@param id` description calls the value a "unique identifier" even though uniqueness is not established by the observable implementation, so it invents a contract. The remaining 4-point deduction reflects that inaccuracy.
- **document-public-api / baseline / trial-03:** semantic/getter-restraint: `getDailyLimit` is a trivial getter but has a multi-line Javadoc header and a verbose `@return` block. Under the stated standard it should be omitted or documented only with a one-line form; the added documentation appears to over-document the getter.
- **document-public-api / with-skill / trial-01:** semantic/semantic-jury: 0/1 judges voted pass; valid judge scores: [65]; consensus uses the median.
- **document-public-api / with-skill / trial-01:** semantic/getter-restraint: The trivial getDailyLimit getter is over-documented with a multi-line header containing both a description and an @return tag. The expected standard permits omitting this getter's header or using only a one-line form; this verbose documentation fails the restraint criterion and the essential getter condition.
- **document-public-api / with-skill / trial-02:** deterministic/signatures-intact: Expected: no public signature was changed.
- **negative-control-inline-comments / with-skill / trial-03:** semantic/semantic-jury: 0/1 judges voted pass; valid judge scores: [90]; consensus uses the median.
- **negative-control-inline-comments / with-skill / trial-03:** semantic/inline-comment-quality: The comments are brief and useful: one explains the zero-load default for technicians absent from `openCounts`, and the other explains retaining the first technician with the lowest observed load. However, the requested non-obvious initialization step is not documented: `fewest = Integer.MAX_VALUE` is the sentinel that allows the first technician to become the initial candidate. The comments also do not explicitly explain that `fewest` tracks the minimum load while iterating. This leaves the explanation incomplete under the stated WHAT/WHY standard.

## Detailed evidence

Open `trials/` for each disposable workspace, worker trace, Git evidence, and configured grader outputs. `experiment.json` contains the complete machine-readable experiment data.
