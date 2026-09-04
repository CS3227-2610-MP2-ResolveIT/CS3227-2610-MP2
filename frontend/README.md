# ResolveIT frontend

ResolveIT's desktop client is a Java 25 and JavaFX 25 application. This first frontend increment provides sign-in against the existing backend and a small authenticated welcome screen.

## Prerequisites

- Java 25
- The ResolveIT backend running locally

Start the backend in one terminal:

```bash
cd backend
./gradlew bootRun
```

Then start the desktop client in another terminal:

```bash
cd frontend
./gradlew run
```

The client connects to `http://localhost:8080/api/v1` by default. Override that URL when needed:

```bash
RESOLVEIT_API_BASE_URL=https://resolveit.example.com/api/v1 ./gradlew run
```

Use HTTPS outside local development. The access token and signed-in user are retained in memory only and are cleared when the user signs out or closes the application.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Manager | `manager@resolveit.local` | `Manager123!` |
| Technician | `technician1@resolveit.local` | `Technician123!` |
| Employee | `employee01@resolveit.local` | `Employee123!` |

## Tests and build

```bash
./gradlew test
./gradlew build
```

Tests cover client-side validation, configuration, session lifecycle, request/response mapping, and authentication failure handling. The backend does not yet expose the refresh and logout endpoints described by `PROJECT.md`, so sign-out in this increment clears only the local in-memory session.
