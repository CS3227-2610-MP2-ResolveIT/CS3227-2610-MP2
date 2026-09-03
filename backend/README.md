# ResolveIT backend

ResolveIT is a Spring Boot/Java 25 REST API for the help-desk application. It is a single monolithic backend containing authentication, user management, tickets, and ticket messages. Data is persisted in SQLite through Spring Data JPA.

## Run

From this directory:

```bash
./gradlew bootRun
```

The API starts at `http://localhost:8080/api/v1` by default. The SQLite database is created as `resolveit.db`; set `RESOLVEIT_DB_PATH` to use another location.

The checked-in JWT secret is for local development only. Outside local development, set `RESOLVEIT_JWT_SECRET` to a base64-encoded secret containing at least 32 bytes.

Run the automated tests with:

```bash
./gradlew test
```

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

## Authentication

Log in to obtain a 15-minute bearer token:

```bash
curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"manager@resolveit.local","password":"Manager123!"}'
```

All endpoints except login require the returned token:

```http
Authorization: Bearer <accessToken>
```

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/auth/login` | Public |
| `GET` | `/auth/me` | Authenticated user |

## User management endpoints

| Method | Endpoint | Access and behavior |
|---|---|---|
| `GET` | `/users?page=0&size=20&role=EMPLOYEE` | Manager; paginated and sorted by ID |
| `POST` | `/users` | Manager; create a user |
| `PATCH` | `/users/{id}` | Manager; update a user |
| `GET` | `/technicians` | Manager; list active technicians and managers by username |

## Ticket endpoints

| Method | Endpoint | Access and behavior |
|---|---|---|
| `POST` | `/tickets` | Any authenticated user; creates an `OPEN`, unassigned ticket for the current user |
| `GET` | `/tickets` | Lists only tickets visible to the current user |
| `GET` | `/tickets/{id}` | Gets a visible ticket; inaccessible tickets return `404` |
| `PATCH` | `/tickets/{id}` | Requester; edits an `OPEN`, unassigned ticket using its current version |
| `POST` | `/tickets/{id}/take` | Technician or manager; atomically takes an `OPEN`, unassigned ticket |
| `POST` | `/tickets/{id}/assign` | Manager; assigns or reassigns an active technician or manager |
| `PATCH` | `/tickets/{id}/status` | Assigned technician or manager; starts work on an assigned `OPEN` ticket |
| `PATCH` | `/tickets/{id}/priority` | Technician or manager; changes ticket priority |
| `POST` | `/tickets/{id}/cancel` | Requester or manager; cancels an `OPEN` or `IN_PROGRESS` ticket |
| `POST` | `/tickets/{id}/reopen` | Requester, technician, or manager; reopens a `RESOLVED` ticket |
| `POST` | `/tickets/{id}/resolve` | Assigned technician or manager; resolves an `IN_PROGRESS` ticket with a note |

Ticket list query parameters:

| Parameter | Default | Description |
|---|---:|---|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Page size from 1 to 100 |
| `status` | — | `OPEN`, `IN_PROGRESS`, `RESOLVED`, or `CANCELLED` |
| `priority` | — | `LOW`, `MEDIUM`, or `HIGH` |
| `assignedToId` | — | Filter by assignee ID; manager only |
| `assignedToMe` | — | Set to `true` for tickets assigned to the current user |
| `unassigned` | — | Set to `true` for unassigned tickets |

Only one assignment filter (`assignedToId`, `assignedToMe=true`, or `unassigned=true`) may be used at a time. Ticket results are sorted by creation time and ID, newest first.

Visibility is role-based:

- Employees see tickets they requested.
- Technicians see their own requested tickets, unresolved queue tickets, and tickets assigned to them.
- Managers see all tickets.

Create a ticket:

```bash
curl -s http://localhost:8080/api/v1/tickets \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "subject":"Cannot connect to Wi-Fi",
    "description":"My laptop reports an authentication error.",
    "category":"NETWORK",
    "priority":"MEDIUM"
  }'
```

Edit a ticket using the `version` returned by the latest ticket response:

```json
{
  "subject": "Cannot connect to office Wi-Fi",
  "priority": "HIGH",
  "version": 0
}
```

Edits use partial-update semantics. At least one of `subject`, `description`, `category`, or `priority` is required. Explicit `null` values and unknown or protected fields are rejected. A stale version returns `409 Conflict`.

Assignment, status, and resolution request bodies:

```json
{"technicianId":12}
```

```json
{"status":"IN_PROGRESS"}
```

```json
{"resolutionNote":"Removed the old wireless profile and connected again."}
```

Reopening a resolved ticket returns it to `OPEN` and clears its assignee, resolution note, and resolution timestamp. Cancelled tickets cannot be reopened.

## Ticket message endpoints

| Method | Endpoint | Access and behavior |
|---|---|---|
| `GET` | `/tickets/{id}/messages?page=0&size=50` | Lists visible messages in creation order; page size is limited to 100 |
| `POST` | `/tickets/{id}/messages` | Adds a message to a ticket the author can view |

Add a public comment:

```json
{
  "messageType": "PUBLIC_COMMENT",
  "message": "The issue still occurs after restarting."
}
```

Technicians and managers may also use `INTERNAL_NOTE`. Employees cannot create internal notes, and internal notes are excluded from all employee message responses.

## Validation and errors

- Ticket subjects contain 5–200 characters after trimming.
- Ticket descriptions contain 10–10,000 characters after trimming.
- Messages contain 1–5,000 characters after trimming.
- Resolution notes contain 1–10,000 characters after trimming.
- Categories are `HARDWARE`, `SOFTWARE`, `NETWORK`, `ACCOUNT_ACCESS`, and `OTHER`.
- Priorities are `LOW`, `MEDIUM`, and `HIGH`.

Errors use a stable JSON structure:

```json
{
  "status": 409,
  "code": "TICKET_VERSION_CONFLICT",
  "message": "The ticket was modified by another request."
}
```

The API uses `400` for invalid requests, `401` for missing or invalid authentication, `403` for insufficient permission, `404` for missing or inaccessible tickets, and `409` for workflow or concurrency conflicts.
