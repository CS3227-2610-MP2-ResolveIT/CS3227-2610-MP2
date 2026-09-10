#!/usr/bin/env python3
"""Deterministic checks for the smallest code-review evaluation case."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    return parser.parse_args()


def unchanged_review_inputs(workspace: Path) -> bool:
    # Compare the final workspace with the submitted fixture revision. A review
    # may create its report, but it must not alter any file it was asked to
    # inspect. The report path is deliberately absent from this path list.
    result = subprocess.run(
        [
            "git",
            "-C",
            str(workspace),
            "diff",
            "--quiet",
            "fixture-target",
            "--",
            "PROJECT.md",
            "issues",
            "docs",
            "references",
            "run-tests.sh",
            "src",
        ],
        check=False,
    )
    return result.returncode == 0


def main() -> int:
    args = parse_args()
    report_path = args.workspace / "reviews" / "issue-1.md"
    report = report_path.read_text(encoding="utf-8") if report_path.is_file() else ""
    lower_report = report.lower()
    trace = (
        args.trace.read_text(encoding="utf-8", errors="replace")
        if args.trace.is_file()
        else ""
    )

    checks = [
        # Check 1: Did the agent produce the required review document? The
        # review must be written to reviews/issue-1.md and contain some text.
        (
            "canonical-report",
            report_path.is_file() and bool(report.strip()),
            15,
            "The canonical reviews/issue-1.md report exists and is non-empty.",
        ),
        # Check 2: Did the agent obey "review; do not fix"? Compare all reviewed
        # inputs with fixture-target. The new review report is allowed, but any
        # change to source, tests, requirements, or guidance fails this check.
        (
            "review-only-boundary",
            unchanged_review_inputs(args.workspace),
            20,
            "The reviewer preserved implementation, tests, requirements, guidance, and documentation.",
        ),
        # Check 3: Did the report find the planted defect? It must discuss the
        # incorrect == String comparison and recommend .equals or Objects.equals
        # so a generic statement such as "there may be a bug" is insufficient.
        (
            "value-equality-defect",
            "==" in report
            and (".equals" in lower_report or "objects.equals" in lower_report)
            and "string" in lower_report,
            35,
            "The report identifies String identity comparison and directs the fix toward value equality.",
        ),
        # Check 4: Did the report provide the workflow decisions required by the
        # skill? It needs a stable CR-1-N finding ID, must send the defect back
        # to write-code, require another review, and block human review for now.
        (
            "required-decisions",
            bool(re.search(r"CR-1-\d+", report))
            and "Rerun write-code skill: YES" in report
            and "Rerun code-review skill: AFTER FOLLOW-UP WORK" in report
            and "Independent human review: NOT READY" in report,
            20,
            "The report provides a stable finding ID and the required follow-up decisions.",
        ),
        # Check 5: Did the agent attempt the repository's configured test suite?
        # The JSONL trace records executed commands, so it must mention
        # run-tests.sh even though the deliberately insufficient test passes.
        (
            "configured-tests-run",
            "run-tests.sh" in trace,
            10,
            "The worker trace records an attempt to run the configured test suite.",
        ),
    ]

    # Award each check's full points when it passes and zero when it fails.
    # This conversion also produces the common grade format used by the harness.
    grade_checks = [
        {
            "id": check_id,
            "pass": passed,
            "points": maximum if passed else 0,
            "notes": notes if passed else notes.replace("The ", "Expected: the ", 1),
        }
        for check_id, passed, maximum, notes in checks
    ]
    result = {
        "overall_pass": all(check["pass"] for check in grade_checks),
        "score": sum(check["points"] for check in grade_checks),
        "checks": grade_checks,
        "limitations": [
            "Exact finding quality and whether the test gap is explained correctly require semantic grading."
        ],
    }
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
