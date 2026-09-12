# ResolveIT Developer Guide

This guide describes the current `0.1.0` codebase. See the
[User Guide](UserGuide.md) for setup and usage.

## Architecture

ResolveIT is a two-process desktop application. The frontend and backend share REST
contracts but no source modules.

```mermaid
flowchart LR
    UI[JavaFX FXML controllers] --> FS[Frontend services]
    FS --> HC[Java HTTP clients]
    HC -->|JSON over REST| RC[Spring REST controllers]
    RC --> BS[Backend services]
    BS --> RP[Spring Data repositories]
    RP --> DB[(SQLite)]
    SEC[Spring Security + JWT] --> RC
```

### Frontend

- `ui` and FXML files define the employee, technician, and manager views.
- `navigation.Navigator` loads views, injects controllers, and disposes the previous
  controller through `ViewLifecycle`.
- `auth`, `ticket`, and `user` services contain validation and use-case logic.
- `HttpAuthClient` and `HttpTicketClient` perform JSON REST calls asynchronously so
  network work does not block the JavaFX Application Thread.
- `SessionState` stores the access token and user in memory only.

### Backend

- REST controllers validate request shapes and delegate to application services.
- `AuthService`, `UserService`, and `TicketService` enforce role permissions,
  visibility, ticket transitions, and other business rules inside transactions.
- Spring Data JPA repositories persist `User`, `Ticket`, and `TicketMessage`
  entities. `schema.sql` owns the schema; Hibernate validates it at startup.
- SQLite uses one pooled connection with foreign keys enabled. Ticket `version`
  provides optimistic-lock protection, while taking a ticket uses an atomic update.
- Spring Security issues 15-minute HS256 JWT access tokens, checks that the user is
  still active, and protects manager endpoints by role. The checked-in secret is
  for local development only.

The API currently implements login and current-user lookup, ticket and message
workflows, and manager user administration. Refresh tokens and server-side logout
are not implemented; frontend sign-out clears local session state. Updates are
manual rather than real-time.

## Software engineering process

[`requirements/PROJECT.md`](../requirements/PROJECT.md) is the product and API source of truth. Work is divided
by role and layer, then implemented with repository-specific backend, JavaFX,
review, logging, and commit skills under [`.agents/skills`](../.agents/skills).
Material AI interactions are summarised under [`logs`](../logs).

Backend integration tests exercise authentication, authorisation, persistence, and
ticket/user workflows. Frontend tests cover validation, session/configuration,
services, HTTP contracts, and error mapping using local test servers. Run:

```bash
cd backend && ./gradlew test
cd ../frontend && ./gradlew test
```

Visible JavaFX behaviour is checked manually; no FXML/UI test remains in the current
suite. GitHub Actions runs the backend and frontend suites as independent CI jobs on
pushes and pull requests, then uploads one cross-platform executable JAR for each
module. Changes should be reviewed against `requirements/PROJECT.md`, tested in the affected
module, documented, and committed as focused Conventional Commits.

## Key extension points

- Add backend behaviour through the matching controller, service, repository, DTO,
  and integration-test path.
- Add frontend behaviour through a service/client interface first, then connect it
  to a controller and FXML view.
- Keep permissions and workflow rules authoritative on the backend; UI restrictions
  are only a usability aid.
- Preserve stable error codes and REST payloads because the desktop client maps
  them to user-facing failures.

## Acknowledgements

- The course specification in [`requirements/MP2-requirements.md`](../requirements/MP2-requirements.md)
  defines the assignment constraints and documentation requirements.
- The backend uses [Spring Boot](https://spring.io/projects/spring-boot/), Spring
  MVC, Spring Data JPA, Spring Security, Hibernate's community SQLite dialect, and
  the [Xerial SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc).
- The frontend uses [OpenJFX](https://openjfx.io/) and
  [Jackson](https://github.com/FasterXML/jackson).
- Builds use the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html),
  whose checked-in generated scripts are provided under the Apache License 2.0.
  Tests use [JUnit](https://junit.org/) and Spring's test libraries.
- The code-review guidance references the
  [SE-EDU Java coding standard](https://se-education.org/guides/conventions/java/)
  and NUS software-engineering guidance linked from the review skill.
- OpenAI Codex assisted with specification refinement, implementation, tests,
  explanations, refactoring, and documentation. The interaction summary is in
  [`logs/summary-1.md`](../logs/summary-1.md).

Beyond the frameworks, generated Gradle Wrapper files, course material, engineering
guidance, and AI assistance listed above, no reused application code or
documentation is recorded in the repository.
