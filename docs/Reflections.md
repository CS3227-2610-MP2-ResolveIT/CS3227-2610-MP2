# Reflections on Basic Agentic Software Engineering

## Reflection by Theodore Lim

## Context and approach

ResolveIT was developed with a single Codex agent customised through repository-level
instructions and task-specific skills. The agent was not treated as an autonomous
replacement for the team. It was used as an implementation, testing, review, and
process assistant whose work remained constrained by [`requirements/PROJECT.md`](../requirements/PROJECT.md),
the course requirements, the existing code, and human decisions.

The main evidence for this reflection is the repository history, the customised
skills under [`.agents/skills`](../.agents/skills), and the verified interaction
summaries under [`logs/`](../logs/). At the time of writing, both
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
that the same reasoning pass silently changes code to match its assumptions. The
skill has now been exercised on a real Manager frontend test review of
`UsersViewTest.java`, which found no meaningful issues. The canonical report
workflow is only partially verified because no canonical
`reviews/issue-<number>.md` report was produced and execution verification was
blocked by Gradle's cache and loopback errors.


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

## Individual reflection — Teo Keng Jer

This reflection covers my recorded JavaFX navigation fix, frontend skill evaluation, and Manager User Management access-cancellation test workflow. My primary evidence is in [window-navigation log](../logs/2026-09-11-201828-javafx-window-navigation-and-skill-evaluation.md) and [Manager cancellation-test log](../logs/2026-09-23-015341-manager-access-cancellation-test.md).

Across these tasks, I set the scope, requested diagnosis before implementation, supplied local verification, and reviewed the interaction summaries. Codex performed the recorded diagnosis, implementation, test correction, evaluation work, and review. The following lessons are retrospective interpretations of that evidence.

### 1. `senior-frontend-javafx-engineer`

I used this skill to investigate window-size resets during top-level navigation and to identify and implement a missing Manager-specific cancellation test. It was appropriate because both tasks depended on JavaFX lifecycle behavior: replacing a Scene during navigation, and closing a confirmation dialog during a test. Its emphasis on small changes, existing project conventions, cancellation states, and explicit verification matched these tasks.

For navigation, Codex traced the reset to `Navigator.show()`, which created a new explicitly sized `Scene(root, 1120, 720)` on every transition. I first requested a diagnosis without edits, then authorized the specific fix: create the Scene once and replace its root afterward. Codex preserved stylesheet loading and controller lifecycle ordering. The production change is recorded in commit `dcb1ef6`.

I personally performed local Gradle verification and manual checks with maximized and manually resized windows. The navigation log does not preserve the exact local command, output, or detailed manual observations, so I cannot claim more precise coverage. I also requested a minimal evaluation fixture and deterministic checker. The recorded evaluation passed one with-skill trial with a score of 100/100. However, its checker used JavaFX doubles: this result verified the modeled navigation interactions, not native window behavior, and did not establish improvement over a without-skill baseline.

The Manager test exposed a limitation in the agent’s first implementation. Codex correctly identified the missing confirmation-cancellation coverage and followed the existing FXML and stub-client patterns, but its cleanup accessed `dialog.getScene()` after Cancel had detached the dialog pane. My local Gradle run exposed the resulting `NullPointerException`. I reported the failure and restricted the correction to test code unless a genuine production defect was demonstrated. Codex then captured the dialog window before firing Cancel and used that reference for cleanup.

My subsequent local run passed the new `UsersViewTest` test and two `AuthenticatedViewTest` tests; the recorded Checkstyle report contained no errors. Codex’s own execution attempts were blocked by cache and loopback errors, so those successful runs must remain attributed to me.

For future work, the improvements supported by these experiences are to check dialog lifecycle assumptions explicitly and preserve local build output and manual observations immediately. The lesson for using one engineering agent is that domain-specific instructions can guide a focused diagnosis and implementation, but they do not eliminate mistakes in the agent’s own tests. Running those tests and returning concrete failure evidence remained essential.

### 2. `code-review`

I used the `code-review` skill after the cancellation test had been corrected. I requested a review of correctness, maintainability, robustness, and consistency with existing project conventions, with no file modifications. This was an appropriate separate task because the implementation needed assessment beyond whether its focused tests passed.

Codex reported no meaningful issues in `UsersViewTest.java`. Its review considered the cancellation assertions, JavaFX threading, fixture setup, stubs, and disposal. The agent respected the requested review boundary and did not make edits.

The evidence for whether this skill worked is narrower than a complete execution-based review. The recorded response addressed the requested concerns and disclosed that Gradle verification was blocked. My earlier successful local test and Checkstyle results provided separate execution evidence; they were not runs completed by the reviewing agent. A lack of review findings also does not prove that every possible defect was absent.

The review had an avoidable execution mistake: its initial command ran from the repository root, where it could not find `gradlew.bat`. After switching to the frontend directory, cache and loopback failures still prevented verification. The logs do not establish a substantive review finding that I needed to correct. They establish a review with explicit execution limitations.

My contribution was to request this additional assessment after the test correction, constrain it to concrete findings, and provide the separate local verification already recorded in the workflow. For future reviews, an improvement supported by this experience is to identify the module’s wrapper location before executing commands and explicitly connect the reviewed change to the available test reports.

This workflow illustrates how a single agent can switch from implementation to review under a different set of instructions. That separation makes its responsibilities clearer, but it does not create an independent human reviewer or replace execution evidence. The useful review outcome was a scoped assessment with disclosed limitations.

### 3. `logging`

I used the `logging` skill to turn these workflows into two detailed, individually attributed records. In the context of MP2, I used the skill to create and verify summaries of prompts and agent interactions, and the existing broad summary did not explicitly capture these tasks, their failures, or the division between my verification and Codex’s work.

The skill organized each record around a coherent task, preserving material prompts, decisions, actions, verification, limitations, and reflection notes. I required `Student owner: Teo Keng Jer`, links to related summary material, and clear separation between my actions and the agent’s actions. I also required that historical details be supported rather than reconstructed by guesswork.

I checked the resulting drafts by reviewing them and explicitly approving their contents before they were written. Both logs record `Human verification: approved`. Repository history records the navigation log in commit `22c62ab` and the cancellation-test log in `258a618`.

I also evaluated whether the logging skill followed its own workflow: coherent task scope, evidence-based reconstruction, clear attribution, presentation of the full draft before writing, and explicit human approval. These checks assessed the skill’s execution as well as the accuracy of its final summaries.

The agent’s useful contribution was assembling evidence from session records, code, commits, evaluation artifacts, and local reports into a readable sequence. It preserved distinctions that matter to the reflection: an attempted check versus a successful check, a source review versus execution, and an automated JavaFX-double evaluation versus native GUI verification.

The limitation was incomplete historical evidence. The navigation workflow lacked the exact local Gradle output and detailed manual observations, while the cancellation workflow did not preserve the complete local command and console transcript. The logs disclosed these gaps instead of inventing them. No content correction to the drafts is recorded before my approval; the evidence therefore supports describing the logging process and its limits, rather than claiming an undocumented logging failure.

Next time, the improvement supported by both logs is to record interactions near the time of the work, including exact commands, relevant outputs, manual observations, and failed-run diagnostics. Generated reports can be overwritten, so preserving their material results promptly would make later reflection more reliable.

The lesson for a single software-engineering agent is that reporting needs its own instructions and human verification. The agent can organize the evidence, but I remain responsible for checking attribution and confirming that the summary accurately distinguishes what I decided and verified from what the agent performed.

Across these examples, skill selection matched the responsibility: JavaFX engineering for diagnosis and implementation, code review for assessment, and logging for preserving verified evidence. Skill execution needed separate checks: local tests and manual verification exposed implementation limits, the review disclosed incomplete execution, and the logging process required evidence checks and my approval. Choosing an appropriate skill established the scope and checking how the agent followed it established what I could reasonably trust.


## Individual reflection — Ng Jun Hao

### My approach to one agent with several narrow skills

For ResolveIT, I did not want one vague instruction telling an agent to “help
build the project.” That would make it easy for the agent to blur planning,
implementation, review, and Git actions together. Instead, I treated the agent
as one assistant that could switch between narrow skills. Each skill had a
specific job, a boundary, a definition of done, and a point where it had to
return control to me.

In this reflection, I discuss `senior-frontend-javafx-engineer`,
`javadoc-writing`, `commit`, and `code-review`. They address four recurring but
different needs in our project. The frontend skill supported my Technician role
work; Javadoc work needed a consistent standard without creating
useless comment clutter. Commit work needed safety because a Git mistake can
include a secret, combine unrelated work, or rewrite history. Code review needed
to be separate from implementation so that the agent did not silently “fix” the
code it was supposed to assess. These were real workflow problems, not four
arbitrary examples selected after the fact.

These were not the only skills in the project. The backend-engineering and
logging skills supported shared implementation and traceability across the
workflow. I focus on these four because I used, refined, or evaluated them with
the clearest reproducible evidence. Together, they show four different ways an
agent needs help: implementing a role-specific workflow, applying a quality
standard, performing a risky repository action, and assessing work without
changing it.

The way I wrote the skills mattered as much as the task list. I kept the main
instructions focused on what every run needs: a short trigger, an ordered
workflow, clear boundaries, and a checkable completion condition. I kept deeper
or branch-specific material behind references. For example, the code-review
skill points to separate files for project checks, detailed review criteria, and
the handoff format. Those references in turn point to the relevant CS2103/T
and SE-EDU standards instead of copying them repeatedly. This progressive
disclosure keeps the main workflow readable and reduces the chance that copied
rules become stale.

I also decided that a skill should not be judged from one convenient run. I
built a small evaluation harness around disposable project-shaped fixtures. It
uses the `codex exec --json` trace approach taught in Lecture 4 to run a plain
baseline and a with-skill configuration, preserve traces and changed-file
evidence, and combine deterministic checks with semantic LLM judges. It also
resumes the same Codex thread for ordered follow-up messages, so an evaluation
can test a workflow over several turns rather than only one final response.
This follows the distinction in Lectures 4 and 5 between observable workflow
facts and qualitative judgement. It was especially useful because the semantic
judges were also LLMs: one score is a sample, not a final truth. The harness
made repeated trials practical through `--parallel`, although the commit
experiments also taught me that parallel workers cannot safely share stateful
Git metadata.

#### Reflection map

| Skill | Workflow problem | Evidence and evaluation result | Main lesson |
|---|---|---|---|
| `senior-frontend-javafx-engineer` | Delivering and preserving a role-specific JavaFX workflow | Technician controller split, focused UI/API tests, review, and manual checks; one narrow with-skill evaluation passed | Repeated JavaFX lifecycle and role-state risks justify dedicated technical context, even without a measured baseline delta. |
| `javadoc-writing` | Useful documentation without clutter | **+24.66 points** on the public-API task; safe no-change and scope behaviour preserved | The baseline's over-documentation and invented contracts show that a focused restraint standard adds value. |
| `commit` | Safe, reviewable Git actions | Safety and approval behaviour improved by **14.5–42.5 points** across the relevant cases | The baseline's unsafe shortcuts show that approval gates and grouping rules need their own skill. |
| `code-review` | Evidence-based review without silent fixes | One skill-guided review met the semantic standard, but no baseline was run | Review remains a separate responsibility, but a baseline rerun is needed before claiming a measured improvement. |

### 1. `senior-frontend-javafx-engineer`: applying the skill to Technician work

I used the frontend workflow while working on the IT support Technician
role, where a correct UI means more than a screen that displays tickets. The
role needs a usable queue and ticket detail flow, while preserving loading,
disabled, confirmation, failure, stale-result, and role-sensitive states for
actions such as taking work, changing status or priority, adding internal notes,
and resolving tickets.

The skill gave the agent a useful checklist for this work: trace the full JavaFX
journey, keep blocking work off the JavaFX Application Thread, preserve FXML and
controller lifecycles, and test keyboard, recovery, and disposal behaviour as
well as the happy path. I applied that guidance during the Technician refactor.
The original controller was split into a workspace shell, queue controller, and
ticket-detail controller; related work made asynchronous lifecycle handling and
ticket-detail loading more explicit. I deliberately kept the Technician boundary
separate rather than forcing it into the Employee flow.

The following project artefacts show where I applied that workflow:

| Technician concern | Main implementation files | Evidence of the applied workflow |
|---|---|---|
| Workspace composition and navigation | `frontend/src/main/java/resolveit/frontend/ui/TechnicianController.java` and `frontend/src/main/resources/resolveit/frontend/views/technician.fxml` | The workspace shell owns navigation and includes the queue and detail views instead of holding their interaction logic itself. |
| Queue states and accessibility | `frontend/src/main/java/resolveit/frontend/ui/TechnicianQueueController.java` and `frontend/src/main/resources/resolveit/frontend/views/technician-queue.fxml` | Status, priority, and assignment filters; loading/error/empty states; pagination; keyboard-accessible queue opening; and responsive table layout are explicit. |
| Selected-ticket workflow | `frontend/src/main/java/resolveit/frontend/ui/TechnicianTicketDetailController.java` and `frontend/src/main/resources/resolveit/frontend/views/technician-ticket-detail.fxml` | Take, begin work, status/priority actions, resolution notes, public/internal messages, confirmations, notices, and role-sensitive controls remain in one detail boundary. |
| Async lifecycle and rendering | `frontend/src/main/java/resolveit/frontend/ui/AsyncOperationTracker.java`, `TechnicianTicketDetailLoader.java`, and `TechnicianTicketDetailRenderer.java` (same package) | In-flight work, paired detail/message loading, cancellation on disposal, stale-result handling, and rendering were extracted from the workspace shell. |
| UI and policy verification | `frontend/src/test/java/resolveit/frontend/ui/TechnicianViewTest.java` and `backend/src/test/java/resolveit/ticket/TicketApiIntegrationTest.java` | Focused tests cover queue recovery, stale-detail disabling, keyboard opening, disposal cancellation, Manager child-controller configuration, Technician action/priority/queue-filter rules, and internal-note visibility. |

The workspace review found unreadable nested FXML, undocumented public
constructors, and missing Manager child-controller coverage, which were then
fixed and rechecked with frontend tests and native Technician/Manager smoke
checks. The existing
[`preserve-window-size` evaluation](../.agents/evals/senior-frontend-javafx-engineer/)
also passed a narrow navigation/lifecycle case, but I would not claim it proves
the whole Technician workflow or a baseline-versus-skill improvement. Next time
I would add Technician-specific evaluation cases for an eligible ticket-taking
flow, a forbidden role action, and invalid resolution data.

What I learned from this work is that a frontend skill is valuable less because
it can generate FXML quickly than because it gives the agent—and me—a way to
reason about the states that are easy to miss. I could have judged the workspace
from the visible happy path, yet stale data, disposal, keyboard opening, and the
Manager child-controller path were exactly where the refactor could have failed.
The skill made those risks routine questions, but I still had to decide the
responsibility boundary and personally check the native behaviour.

### 2. `javadoc-writing`: useful documentation requires restraint

#### Task and skill design

I created `javadoc-writing` after noticing that “add comments” was the wrong
task description. The project did not need commentary everywhere. It needed
good documentation for changed public APIs and non-trivial methods, while
leaving obvious getters, setters, tests, and inherited overrides alone. The
central rule became: explain *what* an API does and, when it matters, *why* it
exists; the code can usually show *how* it works.

I defined the skill around that scope. Before writing, the agent has to identify
the exact target members and read enough code to describe only behaviour that
is actually supported. It must not change signatures, logic, or unrelated files.
Its completion condition is concrete: all appropriate in-scope members are
documented, the diff contains only in-scope Javadocs, and the required Javadoc
and compilation checks are reported. I reduced the skill from 97 to 59 lines
while writing it. Removing repeated explanation made the important instructions
easier to find whenever the skill was loaded; adding more text would not have
made the scope clearer. The writing rules are grounded in the CS2103/SE-EDU Java
standard: accurate present-tense summaries, useful tags only when they add
information, and no fabricated API contract.

This was applied to ResolveIT rather than kept as an abstract exercise. The
resulting documentation change covered the Technician ticket services and
validator, the ticket and user REST DTO contracts, and the non-trivial JavaFX
controller methods introduced around the workspace split. Keeping that change
documentation-only made its reviewable contract clear: behaviour and public
signatures were not supposed to change.

This changed how I think about documentation work. I initially framed it as a
coverage problem—finding members without comments—but the useful question was
whether a future maintainer would learn a real contract from the text. The
CS2103/SE-EDU guidance gave the agent a consistent form, while I still had to
decide when the code was already clearer than another Javadoc block.

#### Evaluation and observed delta

To verify the skill, I designed a [four-case suite](../.agents/evals/javadoc-writing/): a normal public-API task, a scope-boundary task, an
already-correct no-change task, and a negative control where an inline comment
should not trigger Javadoc work. This was my application of Lecture 5's idea of
a representative task dataset. I also redesigned the fixtures so the coding
standard lived in the skill and the private rubric, rather than leaking into the
baseline prompt. That made the comparison a fairer test of what the skill added.

The [experiment-008 report](../.agents/evals/javadoc-writing/runs/experiment-008/report.md)
showed the intended delta on the public-API task: the skill improved its mean
score by **24.66 points**. The baseline tended to document trivial getters too
heavily or invent a contract; the skill more consistently focused on the API
members that needed explanation. It also preserved the already-perfect no-change
and scope-boundary results. That is important: the skill was useful not because
it made more edits, but because it made the appropriate edits more reliably.
This is why Javadoc deserved its own skill: a generic request to “add comments”
does not reliably encode the distinction between necessary documentation and
comment clutter.

#### Human correction and next iteration

However, I would not describe the skill as solved. The strict experiment result
was still **FAIL**. One skill-guided run over-documented
a trivial getter, and a deterministic signature check falsely failed because it
was too sensitive to formatting. I had to decide what documentation was useful,
check that comments had not invented contracts, and interpret the difference
between structural and semantic evidence. Next time I would add more
trivial-member cases and test the grader itself against known-good and known-bad
examples. The [authoring log](../logs/2026-09-25-093700-javadoc-skill-authoring.md)
and [evaluation log](../logs/2026-09-25-113000-javadoc-skill-evaluation.md)
contain the full design and evaluation details.

### 3. `commit`: approval has to be part of the workflow

#### Task and skill design

I created the `commit` skill because Git actions can do more damage than they
appear to. A request to “commit my work” may contain a secret file, personal
notes, unrelated changes, or a request to push or rewrite history. The agent
therefore has to inspect the tree and `.gitignore`, group work by logical
outcome, stage explicit paths, show the whole proposed commit sequence, and
wait for approval before staging and explicit confirmation before each commit.
Pushing, history rewriting, tagging, and issue closing are outside the skill's
scope.

This made Lecture 5's guardrails and approval gates concrete. It also reflects
Lecture 6's point that tool-using agents need bounded authority: the skill does
not assume that silence means approval, and it does not treat a Git command as
safe just because the user mentioned it in the same request. The per-commit
confirmation is particularly important. After seeing the proposed message,
staged paths, and remaining changes, I must explicitly confirm the exact commit
before the agent creates it. A general approval of the plan is not enough. This
makes my review an actual approval gate rather than a blanket authorisation the
agent can reuse later.

I used this workflow throughout the project work rather than only in the
evaluation fixture. Before committing a coherent Technician change, the skill
inspected the worktree and `.gitignore`, proposed explicit paths, and presented
a Conventional Commit type, concise imperative description, and—when the change
was non-trivial—a body explaining what changed and why. This made the test work
and the controller/FXML refactor separately reviewable instead of hiding them in
one large change.

I also refined the skill after seeing that “group by intent” was too vague. It
now keeps implementation with its direct tests and inseparable documentation,
but separates independently reviewable outcomes such as reusable tooling, test
scenarios that consume it, and generated evidence. I later made the preference
for small, focused commits explicit. This was a practical correction: an agent
needs a decision rule for a mixed worktree, not just a slogan about clean Git
history.

This also changed my view of a “good commit.” I first thought the main benefit
was a more readable log. In practice, the harder value was forcing a pause before
a state-changing operation: I had to inspect the proposed paths and rationale,
notice unrelated work, and consciously authorise the next irreversible step.
The skill turned Git from a final mechanical action into a review checkpoint.

#### Evaluation and observed delta

I built a [six-case suite](../.agents/evals/commit/) that included normal grouping, a clean-tree no-op, secret protection, pressure to
skip approval, and requests to push or rewrite history. While designing it, I
realised that my first deterministic graders were attempting to judge things
they could not reliably infer, such as whether a commit grouping was sensible.
I changed them to grade the trace instead: which commands ran, in what order,
and in which turn. Semantic LLM judges handled the genuinely qualitative
questions about grouping, commit messages, trailers, and refusals. This was a
much better use of Lecture 4's distinction between observable workflow evidence
and semantic judgement. The multi-turn cases made this possible: the harness
first lets the agent inspect and propose a plan, then sends a plan-approval
message that still requires an immediate confirmation, and only then sends the
specific confirmation. The trajectory grader can therefore check that no
`git commit` occurred before the correct human approval turn.

The [experiment-007 report](../.agents/evals/commit/runs/experiment-007/report.md)
showed the clearest safety deltas. Compared with the baseline, destructive-
request refusal improved by **42.5 points**, secret protection by **38.5**,
resistance to approval pressure by **31.5**, and grouping/protection of changes
by **14.5**. The already-perfect no-op behaviour was preserved, so the skill did
not create a commit when there was no work to commit. The single-focused-change
case instead fell slightly by **2.5 points**, and the later trace evidence
showed that Git metadata locking rather than the proposed workflow prevented
several commits from completing. In other words, the skill improved the safety
and planning behaviour most clearly; the run was less reliable evidence about
successful Git execution. Those safety deltas are also the reason that commit
work deserved a dedicated skill: a general agent could create a plausible commit
message, but it did not reliably hold approval gates, protect secrets, refuse
out-of-scope history operations, or separate independently reviewable work.

#### Human correction and next iteration

The result also exposed the limits of the harness. The strict outcome was
**FAIL**. Parallel workers shared Git state, and several trials hit
`.git/index.lock` before a real commit could be created. The run still showed
useful directional improvements, but it could not support a strong absolute
pass-rate claim. I also corrected a fixture whose
literal `\\n` was not a real Git trailer, and moved qualitative expectations out
of deterministic checks. Next time I would give each worker an isolated
repository or run workers serially, while retaining parallel read-only judges
and using more trials or a multi-judge jury. The [suite-authoring log](../logs/2026-09-26-112100-commit-skill-eval-suite-authoring.md)
and [run log](../logs/2026-09-26-121300-commit-eval-trajectory-graders-and-run.md)
record those design decisions.

### 4. `code-review`: the evaluator needs review too

#### Task and skill design

I used `code-review` because review should have a different responsibility from
implementation. The skill identifies the scope and relevant requirements,
inspects code, tests, documentation, and the diff, runs applicable checks,
creates evidence-based findings with stable IDs, and states what should happen
next. Its main boundary is **review; do not fix**. That separation makes the
review output inspectable and gives the next implementation or documentation
step a clear handoff. The report also tells the human which manual checks remain,
such as visual layout, native-dialog, keyboard-flow, or platform-specific checks
that source review and automated tests cannot establish. The agent prepares the
evidence and checklist, but does not claim to have performed human acceptance
work.

This skill is also the clearest example of progressive disclosure. Its main
file contains the common review flow. At the appropriate steps, it routes the
agent to project-check, review-criteria, and report references. This avoids
hiding the everyday review workflow inside a huge list of every possible quality
rule, while still giving the agent detailed standards when it needs them.

I also applied this separation to the actual Technician workspace split. The
review of the queue/detail controller and nested-FXML refactor found three
concrete quality gaps: unreadable nested FXML, missing documentation on changed
public constructors, and no automated Manager child-controller path. The fixes
were intentionally performed after the review rather than silently inside it,
then rechecked with frontend tests and native Technician/Manager smoke checks.
That was the practical value of a review handoff: it turned a large refactor
into specific, independently verifiable follow-up work. I used this as a
recurring quality gate across major Technician milestones—workspace refinement,
detail-loading extraction, and the controller split—not only as a final review.

This made me less willing to treat a green test run as a complete quality signal.
The review found maintainability and coverage problems that compilation alone
would not have surfaced, while the manual checklist kept the agent from claiming
native UI acceptance that it could not perform. The useful outcome was not an
agent declaring the work “good”; it was a clearer list of what I still needed to
judge or verify.

#### Evaluation and observed delta

I tested the skill with a [seeded `String` identity-comparison case](../.agents/evals/code-review/test-cases/detect-string-comparison/).
The intended workflow delta was from an unstructured opinion to a review that
identifies the defect, explains why an interned-string test missed it, leaves
the reviewed files unchanged, and produces an actionable report and manual-check
handoff. However, I need to be precise about the evidence: the
[experiment-001 report](../.agents/evals/code-review/runs/experiment-001/report.md)
ran only the with-skill configuration, so it does **not** measure a numerical
baseline-versus-skill delta. It verified that one skill-guided review could
perform the intended work, but it cannot by itself show how much the skill
improved the result.

The run still gave the most useful lesson in the whole evaluation work. The
semantic judges considered the review correct, but the strict result was
**FAIL** because the deterministic oracle required a specific `.equals` phrase even
though the report correctly recommended value-based comparison.

#### Human correction and next iteration

I corrected that brittle wording check so it accepts equivalent value-comparison
terminology. I do not claim that this correction
made the skill pass; it still needs a clean rerun, this time with a matching
baseline configuration. The agent was useful for creating the fixture, checkers,
and structured report, but I had to notice that the evaluation was testing
phrasing too narrowly rather than the intended review behaviour. This taught me
that evaluation infrastructure is also software. It needs known-good and
known-bad cases, review of its own assumptions, and honest reporting when the
evidence is incomplete.

### What I learned about designing an effective single agent

The agent handled bounded, repeatable work well: applying a JavaFX workflow to
Technician work, applying a scoped documentation standard, preparing a safe
local-commit plan, and structuring a review and handoff. This improved
consistency and reduced the repetitive work of re-reading standards, checking
common conditions, and reconstructing JavaFX, Git, or review procedures. It did
not remove the need for my judgement. I still had to choose the right skill,
define the role scope and approval boundary, check quality claims, correct flawed
fixtures and graders, and decide whether the evidence was sufficient.

My main lesson is that an effective single agent is not one that receives a
huge prompt and works independently. It is one that changes mode through narrow
skills, receives the relevant context at the right time, has checkable
completion conditions and safety limits, produces inspectable evidence, and
stops for human judgement at scope, approval, manual-acceptance, and
interpretation boundaries.
