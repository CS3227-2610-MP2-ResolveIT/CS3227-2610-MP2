# Reflections on Basic Agentic Software Engineering

## Context and approach

ResolveIT was developed with a single Codex agent customised through repository-level
instructions and task-specific skills. The agent was not treated as an autonomous
replacement for the team. It was used as an implementation, testing, review, and
process assistant whose work remained constrained by [`requirements/PROJECT.md`](../requirements/PROJECT.md),
the course requirements, the existing code, and human decisions.

The main evidence for this reflection is the repository history, the customised
skills under [`.agents/skills`](../.agents/skills), and the verified interaction
summary in [`logs/summary-1.md`](../logs/summary-1.md). At the time of writing, both
the backend and frontend `./gradlew test` commands pass. This confirms the current
automated suites, but it does not prove unautomated visual or end-to-end behaviour.

## Tasks and selection of the skill set

The agent was customised for five recurring task types:

1. **Backend engineering.** This covered the Spring Boot REST API, SQLite/JPA
   persistence, authentication and authorisation, ticket workflow rules, optimistic
   locking, validation, and backend integration tests.
2. **JavaFX frontend engineering.** This covered login and role-specific employee,
   technician, and manager workspaces, FXML/CSS, HTTP client integration,
   asynchronous UI work, validation, and frontend tests.
3. **Code review.** This separated inspection from implementation and required the
   reviewer to map requirements to code, tests, documentation, and observed
   behaviour before reporting findings.
4. **Interaction logging.** This preserved material prompts, corrections, actions,
   verification, and human approval so that the final reflection could be based on
   evidence rather than memory.
5. **Commit preparation.** This introduced focused Conventional Commit grouping,
   explicit-path staging, pre-commit checks, and human confirmation before each
   commit.

The skill set was derived from the architecture and development workflow rather
than from a generic list of AI capabilities. ResolveIT has a clear backend/frontend
boundary, so backend and JavaFX work needed different technical constraints. The
course also requires production-level testing, documentation, software-engineering
practice, and reflection; this motivated separate review, logging, and commit
skills. The scope of each skill was deliberately narrow so that the same agent
could change modes without mixing responsibilities—for example, the review skill
explicitly says to review but not fix.

## Detailed skill examples

### 1. Senior Backend Java Engineer

The backend skill defines how the agent should trace a request from its REST
contract through domain rules, persistence, security, and tests before editing. It
emphasises server-side enforcement of permissions, transaction boundaries around
business invariants, stable error responses, safe handling of secrets, and tests at
the lowest level that proves the behaviour. These rules match ResolveIT's most
important risks: role-based access, ticket-state transitions, assignment rules,
SQLite persistence, and concurrent edits.

This skill was exercised while creating the user, authentication, ticket, and
ticket-message services. Its effect is visible in integration tests such as
`UserApiIntegrationTest` and `TicketApiIntegrationTest`, and in the use of a ticket
version for optimistic locking. It was also useful for explanation tasks: the agent
traced data from SQLite to the REST API and explained assignee validation, ticket
visibility, and concurrency behaviour. The current backend test suite passes, which
verifies the implemented integration cases. However, a passing suite is only
evidence for the cases present in that suite; it is not proof that every security or
workflow boundary has been covered.

### 2. Senior Frontend JavaFX Engineer

The JavaFX skill was defined specifically for desktop presentation work. It tells
the agent to trace complete user journeys and consider loading, empty, populated,
validation, error, cancellation, and disabled states. It also defines the JavaFX
Application Thread as a boundary, keeps blocking HTTP work off that thread, requires
care around controller and view lifecycles, and asks for FXML, CSS, accessibility,
resizing, and keyboard behaviour to be checked.

This provides the clearest example of defining and verifying a skill. The skill was
added before the employee, technician, and manager workspaces were developed, then
used to guide those implementations. Verification occurred at several levels:

- HTTP client tests check request paths, methods, authentication headers, filters,
  payloads, and error mapping.
- Validator and service tests check login, ticket, session, configuration, and
  manager-user rules without needing to launch JavaFX.
- The team inspected visible behaviour and requested corrections for hover text,
  unwanted sections, bottom whitespace, and unused table space.
- The frontend Gradle test suite currently passes.

### 3. Code Review

The code-review skill was designed as an independent quality gate. It defines an
evidence hierarchy headed by observed behaviour and approved acceptance criteria,
then `requirements/PROJECT.md`, the MP2 requirements, current guides, and finally general
engineering guidance. It requires compilation, tests, realistic workflows, code
quality, security, documentation consistency, and explicit limitations. Findings
must have stable identifiers, severity, reproduction evidence, fix direction, and
post-fix checks. The report must also state whether implementation, documentation,
or another review run is required.

This is an interesting skill because its most important instruction is a boundary:
**review; do not fix**. Separating review from implementation reduces the chance
that the same reasoning pass silently changes code to match its assumptions. At
this stage, the repository contains the skill definition but no canonical
`reviews/issue-<number>.md` report, so it would be inaccurate to claim that the full
review workflow has been verified in practice. Its structure is promising, but the
next project phase should exercise it on a real issue and evaluate whether its
findings are accurate and actionable.


## What the agent handled effectively

The agent was most effective when the task had a clear contract and a bounded
technical surface. It translated `requirements/PROJECT.md` into backend APIs and three
role-specific frontend workflows, connected the JavaFX client to the REST API, and
created repeatable tests for backend integration, client validation, session state,
configuration, HTTP request/response mapping, filtering, manager operations, and
failure handling. This reduced the time needed to produce repetitive DTOs, client
methods, validation cases, and test scaffolding while keeping the implementation
aligned with a shared specification.

The agent also improved productivity as an explainer. Questions about the ticket
version, SQLite file location, package structure, Gradle tasks, configuration, and
foreign-key enforcement were answered in the context of the actual repository.
This made architectural decisions inspectable and helped the team challenge
inconsistencies instead of accepting generated code as a black box.

Code quality benefited most when skill instructions and human review reinforced
each other. For example, after the team questioned inconsistent manager-service
construction, the frontend was refactored so `ManagerService` was created at the
composition root and passed through constructors. This made ownership explicit and
matched the existing dependency-injection style. Testing efficiency improved
because service and HTTP-contract tests exercised most logic without requiring a
full GUI launch.

## Where additional guidance or correction was needed

The interaction history shows that the agent did not reliably infer project taste
or usability requirements from the broad specification alone. It needed direct
correction for package naming, unwanted whitespace, hover-text contrast, removal of
an unnecessary “urgent help” section, and extra space in tables. These are small
changes individually, but together they show that UI acceptance criteria need to be
more concrete than “simple” or “professional.”

The agent also introduced an inconsistent dependency path for `ManagerService`,
using a setter and exposing the service through `Navigator`. Human questioning led
to constructor injection and composition-root construction. This was a valuable
correction because the first version worked functionally but did not follow the
project's established design consistently.

Finally, test strategy required more discipline. The agent created a large JavaFX
wiring test, but it was later removed. The resulting suite still passes, yet it has
lost automated protection against mismatched `fx:id` values, controller factories,
or missing resources. The lesson is not that every UI detail needs an automated
test; it is that the team and agent should agree beforehand which UI risks need
automation, which need a repeatable manual checklist, and why a test may be safely
removed.

## Changes for the next iteration

The next version of the agent instructions and skills should:

- add a project-specific acceptance matrix for each role, including permissions,
  empty/loading/error states, keyboard behaviour, and representative window sizes;
- require the agent to identify and follow the existing dependency-construction
  pattern before adding a new service or controller;
- require at least an FXML load/wiring check for every workspace, while keeping
  screenshot or interaction testing proportionate and documenting any deliberate
  omissions;
- require a short manual visual checklist for CSS or layout changes and record the
  exact sizes and role flows checked;
- run the code-review skill on each completed issue before committing and preserve
  its canonical report as evidence;
- invoke the logging skill per coherent development task instead of reconstructing
  one broad summary near the end; and
- apply the commit skill consistently so messages and commit boundaries remain
  reviewable from the start of the project.

The technical skills could also be made more project-specific by linking directly
to a compact ResolveIT checklist of role permissions, workflow transitions, API
errors, and optimistic-lock cases. This would reduce repeated rediscovery while
keeping `requirements/PROJECT.md` as the authoritative source.

## Lessons about designing one effective AI engineering agent

The strongest design was not one enormous instruction asking the agent to “build
the application.” It was one agent with several bounded roles, a clear source of
truth, and explicit handoff points. Skills were most useful when they described
observable behaviour, boundaries, and verification—not merely a persona such as
“senior engineer.” The backend skill was effective because it named contracts,
transactions, authorisation, concurrency, and test levels. The JavaFX skill was
effective because it named thread, lifecycle, FXML, layout, and state concerns.
The review and logging skills added separation and traceability.

At the same time, custom instructions do not guarantee compliance or correctness.
A skill can be well written but unverified, invoked inconsistently, or overridden
by an expedient decision. Green automated tests only establish the behaviour those
tests cover, and generated code can be functionally correct while inconsistent with
the team's design preferences. Effective single-agent use therefore requires a
closed loop: define a narrow skill, give it authoritative project context, exercise
it on a real task, inspect both the result and its evidence, correct the skill or
implementation, and record what changed.

The main lesson is that human judgement remains most valuable at the boundaries:
choosing scope, deciding acceptable UX, challenging architectural inconsistency,
and judging whether verification is sufficient. The AI agent contributed speed and
breadth; the team supplied intent, review, and accountability. That combination was
more effective than either unstructured prompting or unquestioned automation.
