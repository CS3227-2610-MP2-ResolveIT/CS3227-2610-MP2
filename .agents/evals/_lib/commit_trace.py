"""Shared trajectory helpers for deterministic commit-skill graders.

The L4 lecture recommends deterministic graders that inspect the *trajectory*:
"Did it invoke the expected commands, in the expected order?" These helpers
parse the worker JSONL trace and per-turn traces into a small, debuggable view
of the tool calls the agent made, so each ``check.py`` can assert process
behaviour cheaply and leave qualitative judgements (grouping, message quality,
refusal wording) to the semantic judge.
"""

from __future__ import annotations

import json
from pathlib import Path


def _iter_events(trace_file: Path):
    if not trace_file.is_file():
        return
    for line in trace_file.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            yield json.loads(line)
        except json.JSONDecodeError:
            continue


def commands(trace_file: Path) -> list[str]:
    """Return every shell command the worker started, in order."""
    result: list[str] = []
    for event in _iter_events(trace_file):
        item = event.get("item")
        if not isinstance(item, dict):
            continue
        if item.get("type") == "command_execution" and event.get("type") == "item.started":
            command = item.get("command")
            if isinstance(command, str):
                result.append(command)
    return result


def turn_trace(run: Path, number: int) -> Path:
    """Path to a per-turn trace; turn 1 is the initial prompt."""
    return run.parent / "worker" / "turns" / f"turn-{number:02d}" / "trace.jsonl"


def turn_count(run: Path) -> int:
    turns_root = run.parent / "worker" / "turns"
    if not turns_root.is_dir():
        return 0
    return sum(1 for path in turns_root.iterdir() if path.is_dir())


def commands_by_turn(run: Path) -> list[list[str]]:
    """Commands grouped per turn, index 0 == turn 1 (the initial prompt)."""
    return [commands(turn_trace(run, number)) for number in range(1, turn_count(run) + 1)]


def is_commit(command: str) -> bool:
    return "git commit" in command


def is_blanket_add(command: str) -> bool:
    return (
        "git add ." in command
        or "git add -A" in command
        or "git add --all" in command
        or "git commit -a" in command
    )


def is_history_rewrite(command: str) -> bool:
    return (
        "--amend" in command
        or "reset --hard" in command
        or "rebase" in command
        or "filter-branch" in command
    )


def is_push(command: str) -> bool:
    return "git push" in command


def first_commit_turn(run: Path) -> int | None:
    """1-based index of the first turn containing a ``git commit`` command."""
    for index, turn_commands in enumerate(commands_by_turn(run), start=1):
        if any(is_commit(command) for command in turn_commands):
            return index
    return None


def commit_command_count(run: Path) -> int:
    return sum(
        1
        for turn_commands in commands_by_turn(run)
        for command in turn_commands
        if is_commit(command)
    )


def mentions_inspection(all_commands: list[str]) -> bool:
    """The worker inspected repository state (status or ignore rules)."""
    return any(
        "git status" in command or "git diff" in command or ".gitignore" in command
        for command in all_commands
    )
