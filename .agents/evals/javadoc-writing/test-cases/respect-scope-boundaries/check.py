#!/usr/bin/env python3
"""Deterministic checks for the respect-scope-boundaries Javadoc case.

The prompt gives only the scope (document one file); it states no coding
standard. These checks grade observable outcomes.
"""

from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    return parser.parse_args()


def unchanged(workspace: Path, *paths: str) -> bool:
    result = subprocess.run(
        ["git", "-C", str(workspace), "diff", "--quiet", "HEAD", "--", *paths],
        check=False,
    )
    return result.returncode == 0


def read(workspace: Path, rel: str) -> str:
    path = workspace / rel
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def has_javadoc_before(source: str, declaration: str) -> bool:
    lines = source.splitlines()
    for index, line in enumerate(lines):
        if declaration in line:
            cursor = index - 1
            while cursor >= 0 and (not lines[cursor].strip() or lines[cursor].strip().startswith("@")):
                cursor -= 1
            if cursor >= 0 and lines[cursor].strip().endswith("*/"):
                return True
    return False


def skill_read_ok(workspace: Path, trace: str) -> bool:
    if not (workspace / ".agents/skills/javadoc-writing/SKILL.md").is_file():
        return True
    return "javadoc-writing/SKILL.md" in trace


def main() -> int:
    args = parse_args()
    policy = read(args.workspace, "src/resolveit/access/AccessPolicy.java")
    trace = (
        args.trace.read_text(encoding="utf-8", errors="replace")
        if args.trace.is_file()
        else ""
    )

    signatures_intact = all(
        decl in policy
        for decl in (
            "public AccessPolicy(int maxAttempts)",
            "public boolean isLockedOut(int failedAttempts)",
            "public int remainingAttempts(int failedAttempts)",
        )
    )
    class_documented = has_javadoc_before(policy, "public final class AccessPolicy")
    methods_documented = all(
        has_javadoc_before(policy, decl)
        for decl in (
            "public boolean isLockedOut(int failedAttempts)",
            "public int remainingAttempts(int failedAttempts)",
        )
    )

    checks = [
        (
            "in-scope-documented",
            class_documented and methods_documented,
            45,
            "AccessPolicy class and public methods carry header comments.",
        ),
        (
            "out-of-scope-untouched",
            unchanged(args.workspace, "src/resolveit/access/SessionClock.java"),
            35,
            "SessionClock.java was left unchanged.",
        ),
        (
            "signatures-intact",
            signatures_intact,
            20,
            "No AccessPolicy signature was changed.",
        ),
        (
            "skill-read-when-available",
            skill_read_ok(args.workspace, trace),
            0,
            "Trace: the injected skill was read when present (baseline not applicable).",
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
            "Javadoc content quality requires semantic grading.",
            "The disposable fixture has no Gradle build, so javadoc doclint is not run here.",
        ],
    }
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
