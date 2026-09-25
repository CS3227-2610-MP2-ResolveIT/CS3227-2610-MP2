#!/usr/bin/env python3
"""Deterministic checks for the no-change-when-already-documented Javadoc case.

The prompt asks to document an already-documented class. The right outcome is
recognising it needs no substantive change. No coding standard is provided to
the worker.
"""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

TARGET = "src/resolveit/auth/LockoutPolicy.java"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    return parser.parse_args()


def changed_line_count(workspace: Path, path: str) -> int:
    # Added/removed lines for the target versus the prepared workspace base
    # (HEAD). Zero means untouched; a large number means churn.
    result = subprocess.run(
        ["git", "-C", str(workspace), "diff", "--numstat", "HEAD", "--", path],
        check=False,
        capture_output=True,
        text=True,
    )
    line = result.stdout.strip()
    if not line:
        return 0
    added, removed, *_ = line.split("\t")
    added = 0 if added == "-" else int(added)
    removed = 0 if removed == "-" else int(removed)
    return added + removed


def read(workspace: Path, rel: str) -> str:
    path = workspace / rel
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def main() -> int:
    args = parse_args()
    policy = read(args.workspace, TARGET)
    churn = changed_line_count(args.workspace, TARGET)

    signatures_intact = all(
        decl in policy
        for decl in (
            "public boolean isLockedOut(int failures)",
            "public int attemptsLeft(int failures)",
        )
    )

    other_changes = subprocess.run(
        ["git", "-C", str(args.workspace), "diff", "--quiet", "HEAD", "--", ":!" + TARGET],
        check=False,
    ).returncode != 0

    checks = [
        (
            "no-churn",
            churn <= 4,
            60,
            f"The already-correct Javadoc was not churned (changed lines: {churn}).",
        ),
        (
            "signatures-intact",
            signatures_intact,
            20,
            "No public signature was changed.",
        ),
        (
            "no-other-file-changed",
            not other_changes,
            20,
            "No file other than the target was modified.",
        ),
    ]

    grade_checks = [
        {
            "id": cid,
            "pass": passed,
            "points": maximum if passed else 0,
            "notes": notes if passed else "Expected: " + notes[0].lower() + notes[1:],
        }
        for cid, passed, maximum, notes in checks
    ]
    result = {
        "overall_pass": all(c["pass"] for c in grade_checks),
        "score": sum(c["points"] for c in grade_checks),
        "checks": grade_checks,
        "limitations": [
            "Whether the worker reported the no-change outcome honestly requires semantic grading.",
            "The churn threshold is a heuristic; a small standard-compliant edit may be acceptable.",
        ],
    }
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
