#!/usr/bin/env python3
"""Deterministic checks for the document-public-api Javadoc case.

The fixture and prompt carry no coding standard; the standard lives only in the
skill. These checks grade observable outcomes, not the guidance the worker saw.
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
    # True when none of the given paths differ from the prepared workspace base
    # (HEAD), so harness-injected files are not counted as worker changes.
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
    # Trace grader: with-skill injects the skill file, which the worker should
    # read; baseline has no skill file, so the check is not applicable.
    if not (workspace / ".agents/skills/javadoc-writing/SKILL.md").is_file():
        return True
    return "javadoc-writing/SKILL.md" in trace


def main() -> int:
    args = parse_args()
    quota = read(args.workspace, "src/resolveit/ticket/TicketQuota.java")
    ticket = read(args.workspace, "src/resolveit/ticket/Ticket.java")
    trace = (
        args.trace.read_text(encoding="utf-8", errors="replace")
        if args.trace.is_file()
        else ""
    )

    signatures_intact = all(
        decl in quota
        for decl in (
            "public TicketQuota(int dailyLimit)",
            "public int remaining(List<Ticket> todaysTickets)",
            "public void assertCanCreate(List<Ticket> todaysTickets)",
        )
    ) and "public record Ticket(long id, String subject, boolean countsAgainstQuota)" in ticket

    class_documented = has_javadoc_before(quota, "public final class TicketQuota") and (
        has_javadoc_before(ticket, "public record Ticket")
    )
    public_methods_documented = all(
        has_javadoc_before(quota, decl)
        for decl in (
            "public int remaining(List<Ticket> todaysTickets)",
            "public void assertCanCreate(List<Ticket> todaysTickets)",
        )
    )
    exception_documented = "@throws" in quota and "QuotaExceededException" in quota and "@param" in quota

    checks = [
        (
            "in-scope-classes-documented",
            class_documented,
            20,
            "TicketQuota and Ticket carry a class header comment.",
        ),
        (
            "public-methods-documented",
            public_methods_documented,
            25,
            "Public methods of TicketQuota carry header comments.",
        ),
        (
            "param-return-throws",
            exception_documented,
            15,
            "assertCanCreate documents @param and the @throws QuotaExceededException.",
        ),
        (
            "signatures-intact",
            signatures_intact,
            20,
            "No public signature was changed.",
        ),
        (
            "ticket-scope-only",
            unchanged(args.workspace, "src/resolveit/notify"),
            20,
            "Only the ticket package was modified; the notify package was left alone.",
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
            "Javadoc content quality and getter restraint require semantic grading; these structural checks do not detect over-documentation.",
            "The disposable fixture has no Gradle build, so javadoc doclint is not run here.",
        ],
    }
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
