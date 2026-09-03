# ResolveIT backend

Spring Boot/Java 25 MVP backend. The first vertical slice provides the SQLite schema, JWT login, current-user lookup, and manager-only user management REST API.

## Run

```bash
./gradlew bootRun
```

The SQLite database is created as `resolveit.db`. Override its location with `RESOLVEIT_DB_PATH`. The checked-in JWT secret is for local development only; set `RESOLVEIT_JWT_SECRET` to a base64-encoded secret of at least 32 bytes outside local development.

## Demo logins

On an empty database, 34 active users are inserted automatically. Passwords are stored as bcrypt hashes.

| Role | Email | Password |
|---|---|---|
| Manager | `manager@resolveit.local` | `Manager123!` |
| Technician 1 | `technician1@resolveit.local` | `Technician123!` |
| Technician 2 | `technician2@resolveit.local` | `Technician123!` |
| Technician 3 | `technician3@resolveit.local` | `Technician123!` |
| Employees 01–30 | `employee01@resolveit.local` through `employee30@resolveit.local` | `Employee123!` |

These accounts are demo data and must not be used in production.

## Endpoints

- `POST /api/v1/auth/login` — public; returns a 15-minute bearer access token.
- `GET /api/v1/auth/me` — authenticated user.
- `GET /api/v1/users?page=0&size=20&role=EMPLOYEE` — manager; paginated and sorted by ID.
- `POST /api/v1/users` — manager.
- `PATCH /api/v1/users/{id}` — manager.
- `GET /api/v1/technicians` — manager; active technicians and managers sorted by username.

Example login:

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"manager@resolveit.local","password":"Manager123!"}'
```

Send the returned token as `Authorization: Bearer <accessToken>`.
