# ResolveIT User Guide

ResolveIT is a JavaFX desktop help-desk application backed by a Spring Boot API
and SQLite database.

## Requirements and setup

Install Java 25. From the repository root, start the backend:

```bash
cd backend
./gradlew bootRun
```

On first run, it creates `backend/resolveit.db` and inserts demo accounts. In a
second terminal, start the desktop client:

```bash
cd frontend
./gradlew run
```

On Windows, use `gradlew.bat` instead. The client connects to
`http://localhost:8080/api/v1` by default.

| Role | Email | Password |
| --- | --- | --- |
| Employee | `employee01@resolveit.local` | `Employee123!` |
| Technician | `technician1@resolveit.local` | `Technician123!` |
| Manager | `manager@resolveit.local` | `Manager123!` |

## Current features

- **Employees:** submit, view, and filter their tickets; edit open unassigned
  tickets; cancel eligible tickets; reopen resolved tickets; and add public
  comments.
- **Technicians:** view and filter the support queue, take tickets, begin work,
  change priority, resolve assigned work, reopen tickets, and add public comments
  or internal notes.
- **Managers:** perform technician actions, view all tickets, assign tickets, and
  create or edit user accounts, roles, and activation status.

Use the refresh controls to retrieve changes; real-time updates are not supported.
Signing out clears the local session. Demo accounts and the default JWT secret are
for local development only.

## Testing

Run both automated test suites:

```bash
cd backend
./gradlew test

cd ../frontend
./gradlew test
```

Both suites should finish with `BUILD SUCCESSFUL`.

## Packaged application

Successful GitHub Actions runs provide `resolveit-backend-0.1.0.jar` and
`resolveit-frontend-0.1.0.jar` in the `resolveit-cross-platform-jars` artifact.
Both JARs require Java 25 and run on Linux or Windows:

```bash
java -jar resolveit-backend-0.1.0.jar
java --enable-native-access=ALL-UNNAMED -jar resolveit-frontend-0.1.0.jar
```
