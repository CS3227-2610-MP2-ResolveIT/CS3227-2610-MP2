#!/usr/bin/env python3
"""Deterministic checks for the negative-control-inline-comments case.

The prompt asks for inline comments, not Javadoc. The right outcome is inline
`//` comments and NOT an API-documentation header. No standard is provided.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

TARGET = "src/resolveit/ticket/TechnicianRouter.java"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    return parser.parse_args()


def read(workspace: Path, rel: str) -> str:
    path = workspace / rel
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def method_body(source: str) -> str:
    start = source.find("pickTechnician")
    return source[start:] if start != -1 else ""


def main() -> int:
    args = parse_args()
    source = read(args.workspace, TARGET)
    body = method_body(source)

    signature_intact = (
        "public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts)"
        in source
    )
    has_inline_comment = "//" in body
    added_javadoc_block = "/**" in source

    checks = [
        (
            "inline-comments-added",
            has_inline_comment,
            55,
            "Inline // comments were added inside the method body.",
        ),
        (
            "no-javadoc-misfire",
            not added_javadoc_block,
            30,
            "No Javadoc header block was added; the inline-comment task was not treated as API documentation.",
        ),
        (
            "signature-intact",
            signature_intact,
            15,
            "The method signature was preserved.",
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
            "Whether the inline comments are genuinely useful (WHAT/WHY not HOW) requires semantic grading.",
            "The Javadoc-misfire check is structural; a judge assesses whether the worker understood the task.",
        ],
    }
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
