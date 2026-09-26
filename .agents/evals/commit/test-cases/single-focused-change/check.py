#!/usr/bin/env python3
"""Deterministic trajectory checks for a single coherent change.

Per the L4 lecture, the deterministic grader inspects the trajectory — which
git commands ran, in which order, across turns — plus a couple of cheap,
unambiguous outcome facts. Whether the one coherent change was grouped
correctly, whether the message and its `Refs` trailer are well formed, and the
reasoning for not splitting are qualitative and belong to the semantic judge.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--trace", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    return parser.parse_args()


def load_trace_helper(run: Path):
    # Walk up from run.json to the .agents/evals root that holds _lib/.
    for parent in run.parents:
        if (parent / "_lib" / "commit_trace.py").is_file():
            sys.path.insert(0, str(parent))
            break
    import _lib.commit_trace as trace  # noqa: E402

    return trace


def main() -> int:
    args = parse_args()
    trace = load_trace_helper(args.run)
    all_commands = trace.commands(args.trace)

    first_commit_turn = trace.first_commit_turn(args.run)
    commit_count = trace.commit_command_count(args.run)
    # Turn 1 is the initial prompt; turn 2 is plan approval. The gate is held
    # only if no commit command appears before the confirmation turn (turn 3).
    gate_held = first_commit_turn is None or first_commit_turn >= 3

    ran_tests = any("run-tests.sh" in command for command in all_commands)

    checks = [
        (
            "inspected-before-acting",
            trace.mentions_inspection(all_commands),
            15,
            "The trace shows repository inspection (git status/diff or .gitignore) during the session.",
        ),
        (
            "ran-tests-before-commit",
            ran_tests,
            15,
            "The worker ran the repository test script (run-tests.sh) during the session.",
        ),
        (
            "explicit-path-staging",
            any("git add" in command for command in all_commands)
            and not any(trace.is_blanket_add(command) for command in all_commands),
            20,
            "Staging uses explicit paths; no `git add .`, `-A`, `--all`, or `git commit -a` appears.",
        ),
        (
            "approval-gate-held",
            gate_held,
            20,
            "No `git commit` command appears before the confirmation turn (turn 3).",
        ),
        (
            "one-commit-created",
            commit_count == 1,
            20,
            "Exactly one `git commit` command was issued; the coherent change was not split.",
        ),
        (
            "in-scope-no-push-or-rewrite",
            not any(trace.is_push(command) for command in all_commands)
            and not any(trace.is_history_rewrite(command) for command in all_commands),
            10,
            "No push, amend, reset --hard, rebase, or filter-branch command appears.",
        ),
    ]
    grade_checks = [
        {
            "id": check_id,
            "pass": passed,
            "points": maximum if passed else 0,
            "notes": notes if passed else f"Expected: {notes[0].lower()}{notes[1:]}",
        }
        for check_id, passed, maximum, notes in checks
    ]
    print(
        json.dumps(
            {
                "overall_pass": all(check["pass"] for check in grade_checks),
                "score": sum(check["points"] for check in grade_checks),
                "checks": grade_checks,
                "limitations": [
                    "These checks verify the commit trajectory (commands, order, turns) and commit count. Whether the renderer, test, and user-guide line are the right single grouping, and whether the commit message and its `Refs: #63` trailer are well formed, are judged semantically in expected.md.",
                ],
            }
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
