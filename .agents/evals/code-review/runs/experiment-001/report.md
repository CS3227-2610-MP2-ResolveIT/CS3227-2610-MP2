# Evaluation report: code-review

## Result

| Field | Value |
|---|---|
| Outcome | **FAIL** |
| Mean score | 100.0/100 |
| Passed trials | 0/1 |
| Completed trials | 1/1 |
| Experiment status | completed |
| Semantic judges | 3 per eligible trial |

## Trial results

| Test case | Configuration | Trial | Score | Pass | Details |
|---|---|---:|---:|---:|---|
| detect-string-comparison | with-skill | 1 | 100 | No | [result.json](trials/detect-string-comparison/with-skill/trial-01/result.json) |

## Main failure reasons

- **detect-string-comparison / with-skill / trial-01:** deterministic/value-equality-defect: Expected: the report identifies String identity comparison and directs the fix toward value equality.

## Detailed evidence

Open `trials/` for each disposable workspace, worker trace, Git evidence, and configured grader outputs. `experiment.json` contains the complete machine-readable experiment data.
