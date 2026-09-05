---
name: logging
description: Summarize AI-assisted development interactions for the CS3227 MP2 project, asking targeted questions when needed for an accurate log or reflection. Use when the user asks to log a session, record prompts and decisions, verify an interaction summary, or extract reflection points for docs/Reflections.md. Do not use for Java application runtime logging such as Log4j, SLF4J, or java.util.logging.
---

# Logging

Maintain accurate summaries of AI-assisted development work in the repository's `logs/` directory.

Before logging:

- Read `AGENTS.md`, `PROJECT.md`, and `docs/MP2-requirements.md` for every session in this repository. These documents explain the ResolveIT requirements and why these logs exist: to support reflection on basic Agentic SE and at least three explained skills in `docs/Reflections.md`.
- Inspect existing files in `logs/` and follow their naming conventions.
- Do not overwrite an existing log without inspecting it first.

## Ordering

Name every log:

```text
logs/YYYY-MM-DD-HHMMSS-short-description.md
```

Use the repository's local timezone and 24-hour time. The timestamp must be the session's start time when known. If migrating an older log, use the best available repository timestamp and state its source; do not claim it is the exact interaction time. The filename's timestamp is the sort key.

## Session scope and format

One log covers one coherent development task, not one prompt or command.
Establish the interaction range before drafting. Include every material
prompt, AI response, correction, decision, action, and verification in that
range.

Group routine iterations with the same purpose. Split unrelated goals into
separate logs. If a completed task is reopened later, create a linked
follow-up log instead of expanding the old one.

Use this concise format, separating proposed, approved, and executed work:

```markdown
Date: <date>
Time: <time>
Timestamp source: <source>
Task: <coherent development task>
Status: completed | ongoing | blocked | abandoned
Interaction range: <start> – <end>
Evidence sources: <conversation, files, diffs, tool results>
Related logs: <paths or none>
Human verification: pending

## Interaction history

### 1. <meaningful interaction point>

- Exact prompt excerpt or labelled faithful paraphrase:
- AI response: <concise summary or exact excerpt when useful>
- User correction or decision:
- Resulting action:

### 2. <meaningful interaction point>

- Exact prompt excerpt or labelled faithful paraphrase:
- AI response: <concise summary or exact excerpt when useful>
- User correction or decision:
- Resulting action:

## Work and verification

- Proposed, approved, and executed actions:
- Files or external systems changed:
- Checks and observed results:
- Errors, limitations, or remaining uncertainty:

## Reflection notes

- What the AI did well or poorly:
- Human judgement required:
- How the prompts or approach evolved:
- Prompting versus manual work, when relevant:
- What to do differently next time:
```

## Evidence and approval

Build the draft from the available conversation, repository, diffs, and tool
results. Do not ask the user to repeat supported facts or invent private
motivation, unobserved events, approvals, manual verification, or AI behaviour.

Preserve concise exact excerpts of material prompts when practical. Otherwise,
use clearly labelled faithful paraphrases. Include follow-up prompts that
changed scope, corrected the AI, or changed the final decision. Summarize AI
responses rather than reproducing the full transcript.

Ask at most one concise batch about reflection-important personal reasoning or
manual verification that the evidence cannot establish. Use `Unknown` or `Not
applicable` only when the missing information materially affects the summary.

Before writing:

- show the target path and complete draft for a new log;
- show the target path and exact diff for an existing log;
- wait for explicit approval.

Silence is not approval. After approval, write only the approved content under
`logs/`, re-read it, and record:

```text
Human verification: approved
```
