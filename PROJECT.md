# ResolveIT — MVP Project Specification

## 1. Overview

ResolveIT is an internal IT help-desk desktop application. Employees submit and track support tickets, IT support technicians work on those tickets, and IT managers oversee ticket assignment and user accounts.

### Required technology

| Area | Technology |
|---|---|
| Language/runtime | Java 25 |
| Desktop client | JavaFX 25 |
| Backend | Spring Boot on Java 25 |
| Client–server communication | HTTPS REST API |
| Database | SQLite |
| Persistence | Spring Data JPA |
| Authentication and authorization | Spring Security with JWT |
| Build system | Gradle 9 |
| Testing | JUnit 6 |

Real-time communication, Server-Sent Events, and WebSockets are outside the MVP. The client refreshes data on demand, after successful changes, and optionally by periodic polling.

## 2. Scope and roles

### Employee

- Sign in and sign out.
- Submit tickets.
- Edit the subject, description, category, and priority of their own ticket while it is `OPEN` and unassigned.
- View and filter their own tickets.
- View ticket details and status.
- Add public comments to their own tickets.
- Cancel their own `OPEN` or `IN_PROGRESS` tickets.
- Reopen their own `RESOLVED` tickets.

### IT support technician

- Perform the relevant shared actions above.
- View all non-cancelled, unresolved tickets and tickets assigned to them.
- Take an unassigned ticket.
- Change ticket status and priority where permitted.
- Add public comments and internal notes.
- Resolve tickets.

### IT manager

- Perform all technician actions.
- View all tickets, including resolved and cancelled tickets.
- Assign and reassign tickets to active technicians or managers.
- Create, update, activate, and deactivate user accounts.
- Change user roles.

### Explicitly out of scope

Attachments, configurable categories or priorities, dashboards, reports and statistics, escalation workflows, support teams, service-level targets, email notifications, and real-time events are deferred until after the MVP.

## 3. Ticket workflow

### Statuses and transitions

```text
OPEN ───────────► IN_PROGRESS ───────────► RESOLVED
 │                    │                       │
 └────► CANCELLED ◄───┘                       └────► OPEN
```

| Action | From | To | Authorized role |
|---|---|---|---|
| Take an unassigned ticket | `OPEN` | `IN_PROGRESS` | Technician, manager |
| Begin or resume work | `OPEN` | `IN_PROGRESS` | Assigned technician, manager |
| Resolve | `IN_PROGRESS` | `RESOLVED` | Assigned technician, manager |
| Cancel | `OPEN`, `IN_PROGRESS` | `CANCELLED` | Requester, manager |
| Reopen | `RESOLVED` | `OPEN` | Requester, technician, manager |

Rules:

- New tickets start as `OPEN` and unassigned.
- Taking a ticket atomically assigns it to the current technician and changes its status to `IN_PROGRESS`.
- Resolving a ticket requires a non-blank resolution note and sets `resolved_at`.
- Reopening a ticket clears its assignee, resolution note, and `resolved_at` so it returns to the open queue.
- Cancelled tickets cannot be reopened in the MVP.
- A ticket cannot be assigned to an inactive user or an employee.
- The backend validates every transition and permission, regardless of what controls the client displays.

## 4. Core data model

All application timestamps represent UTC instants. Because SQLite has no native time-zone-aware timestamp type, the database schema must store them in a consistently sortable UTC representation supported by the chosen JPA mapping.

For the MVP, keep the database definition in a version-controlled `schema.sql` file and let Spring SQL initialization create a new SQLite database. Configure Hibernate to validate the schema rather than generate or update it automatically. Upgrading an existing database between schema versions is outside the MVP;

### `users`

| Column | Definition |
|---|---|
| `id` | Integer primary key |
| `username` | `VARCHAR(50)`, unique, not null |
| `email` | `VARCHAR(254)`, unique, not null |
| `password_hash` | `VARCHAR(255)`, not null |
| `role` | `VARCHAR(20)`, not null |
| `active` | Boolean, not null, default `true` |
| `created_at` | UTC timestamp, not null |
| `updated_at` | UTC timestamp, not null |

Allowed roles: `EMPLOYEE`, `TECHNICIAN`, `MANAGER`.

### `tickets`

| Column | Definition |
|---|---|
| `id` | Integer primary key |
| `ticket_number` | `VARCHAR(30)`, unique, not null |
| `subject` | `VARCHAR(200)`, not null |
| `description` | Text, not null |
| `category` | `VARCHAR(50)`, not null |
| `priority` | `VARCHAR(20)`, not null |
| `status` | `VARCHAR(20)`, not null |
| `requester_id` | Foreign key to `users.id`, not null |
| `assigned_to_id` | Foreign key to `users.id`, nullable |
| `resolution_note` | Text, nullable |
| `created_at` | UTC timestamp, not null |
| `updated_at` | UTC timestamp, not null |
| `resolved_at` | UTC timestamp, nullable |
| `version` | Integer, not null, default `0`; JPA optimistic-lock field |

Allowed priorities: `LOW`, `MEDIUM`, `HIGH`.

Fixed categories: `HARDWARE`, `SOFTWARE`, `NETWORK`, `ACCOUNT_ACCESS`, `OTHER`.

Categories, priorities, statuses, and roles are enums or fixed application values in the MVP.

### `ticket_messages`

| Column | Definition |
|---|---|
| `id` | Integer primary key |
| `ticket_id` | Foreign key to `tickets.id`, not null |
| `author_id` | Foreign key to `users.id`, not null |
| `message_type` | `VARCHAR(20)`, not null |
| `message` | Text, not null |
| `created_at` | UTC timestamp, not null |

Allowed message types: `PUBLIC_COMMENT`, `INTERNAL_NOTE`.

Internal notes are visible only to technicians and managers. Messages are ordered by `created_at`, then `id`; no separate ordering column is required.

## 5. REST API

The base path is `/api/v1`. Except for login and token refresh, protected requests use:

```http
Authorization: Bearer <access-token>
```

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/auth/login` | Authenticate and issue access and refresh tokens |
| `POST` | `/auth/refresh` | Exchange a valid, non-revoked refresh token |
| `POST` | `/auth/logout` | Revoke the presented refresh token |
| `GET` | `/auth/me` | Return the current user |

### Tickets

| Method | Endpoint | Permission or behavior |
|---|---|---|
| `POST` | `/tickets` | Any authenticated active user; requester is the current user |
| `GET` | `/tickets` | Return only tickets visible to the current user; support status and assignment filters |
| `GET` | `/tickets/{id}` | Requester, technician, or manager, subject to visibility rules |
| `PATCH` | `/tickets/{id}` | Requester; edit permitted fields only while the ticket is `OPEN` and unassigned |
| `POST` | `/tickets/{id}/take` | Technician or manager; ticket must be unassigned and `OPEN` |
| `POST` | `/tickets/{id}/assign` | Manager |
| `PATCH` | `/tickets/{id}/status` | Technician or manager, subject to workflow and assignment rules |
| `PATCH` | `/tickets/{id}/priority` | Technician or manager |
| `POST` | `/tickets/{id}/cancel` | Requester or manager |
| `POST` | `/tickets/{id}/reopen` | Requester, technician, or manager |
| `POST` | `/tickets/{id}/resolve` | Assigned technician or manager |

### Messages

| Method | Endpoint | Permission or behavior |
|---|---|---|
| `GET` | `/tickets/{id}/messages` | List only messages visible to the current user |
| `POST` | `/tickets/{id}/messages` | Add a public comment or, for technicians and managers, an internal note |

### User management

| Method | Endpoint | Permission |
|---|---|---|
| `GET` | `/users` | Manager |
| `POST` | `/users` | Manager |
| `PATCH` | `/users/{id}` | Manager |
| `GET` | `/technicians` | Manager; return active assignable users |

List endpoints should use bounded pagination and stable sorting. Exact query parameters and response DTOs may be defined during implementation but must remain consistent across the backend and client.

## 6. Representative API requests

Create a ticket:

```json
{
  "subject": "Cannot connect to Wi-Fi",
  "description": "My laptop reports an authentication error.",
  "category": "NETWORK",
  "priority": "MEDIUM"
}
```

Edit a ticket:

```json
{
  "subject": "Cannot connect to office Wi-Fi",
  "description": "My laptop reports an authentication error on the office network.",
  "category": "NETWORK",
  "priority": "HIGH",
  "version": 0
}
```

The edit request uses partial-update semantics: omitted fields remain unchanged, while an explicitly supplied `null` is rejected. At least one editable field must be supplied. `ticketNumber`, `requesterId`, assignment, status, resolution fields, timestamps, and message history cannot be changed through this endpoint. The supplied `version` is required and must match the current ticket version; otherwise the backend returns `409 Conflict`.

Add a message:

```json
{
  "messageType": "PUBLIC_COMMENT",
  "message": "The issue still occurs after restarting."
}
```

Assign a ticket:

```json
{
  "technicianId": 12
}
```

Resolve a ticket:

```json
{
  "resolutionNote": "Removed the old wireless profile and connected again."
}
```

Standard error response:

```json
{
  "status": 403,
  "code": "ACCESS_DENIED",
  "message": "You are not allowed to perform this action."
}
```

Use appropriate HTTP statuses, including `400` for invalid input, `401` for missing or invalid authentication, `403` for insufficient permission, `404` for inaccessible or missing resources, and `409` for state or concurrency conflicts.

## 7. JavaFX application

### Shared screens

- Login.
- Main navigation.
- Ticket details.
- Profile and logout.

### Employee screens

**My Tickets**

- Display the employee's tickets.
- Filter by status.
- Open ticket details.
- Refresh manually.

**Submit Ticket**

- Subject, description, category, and priority fields.
- Submit action.

**Ticket Details**

- Display ticket number, fields, status, and assignee.
- Allow the requester to edit permitted fields while the ticket is `OPEN` and unassigned.
- Display and add public comments.
- Display cancel or reopen actions when permitted.

### Technician screens

**Ticket Queue**

- Display open and in-progress tickets.
- Filter unassigned tickets and tickets assigned to the current technician.
- Filter by status and priority.

**Ticket Details**

- Take an eligible ticket.
- Change status and priority.
- Add public comments and internal notes.
- Enter a resolution note and resolve the ticket.

### Manager screens

**All Tickets**

- Display all tickets.
- Assign or reassign technicians.

**User Management**

- List and create users.
- Change roles.
- Activate or deactivate users.

## 8. JavaFX client behavior

- Network requests must not run on the JavaFX Application Thread.
- Show a loading indicator and disable the triggering action while a request is in progress.
- Prevent duplicate submissions.
- Show clear validation, authentication, authorization, conflict, and server errors.
- Refresh affected data after successful changes and provide a manual refresh action.
- The client may poll every 30–60 seconds while an applicable screen is visible.
- Store tokens using the most secure mechanism reasonably available for the target desktop environment and clear them on logout.
- Hide actions unavailable to the current role, while treating the backend as the final authority.
- Use Java 25 virtual threads for suitable blocking HTTP work and marshal UI updates back through `Platform.runLater` or an equivalent JavaFX mechanism.

A simple client structure is sufficient:

```text
FXML view → Controller → Application service → REST client
```

## 9. Security requirements

The MVP must:

- Use HTTPS outside local development.
- Hash passwords with Argon2id or bcrypt; never store plaintext passwords.
- Never log passwords, access tokens, or refresh tokens.
- Validate authentication, authorization, ownership, and ticket transitions in the backend.
- Prevent employees from discovering or accessing other employees' tickets.
- Never include internal notes in employee-facing responses.
- Validate and normalize all request data.
- Rate-limit repeated login attempts.
- Use short-lived access tokens and revocable, expiring refresh tokens.
- Return safe error responses without stack traces or sensitive details.
- Keep secrets and production configuration outside source control.
- Reject authentication for inactive users and prevent their refresh tokens from being used.

Hiding a JavaFX control is not a security measure. Every protected operation must be authorized independently by the backend.

## 10. Validation rules

### User

- Username: 3–50 characters and unique.
- Email: valid format, at most 254 characters, and unique.
- Password: at least 5 characters.
- Role: one of the allowed role values.

### Ticket

- Subject: 5–200 characters after trimming.
- Description: 10–10,000 characters after trimming.
- Category and priority: allowed enum values.
- Ticket edits may change only subject, description, category, and priority, and use the same field validation as ticket creation.
- Only the requester may use the general ticket-edit endpoint, and only while the ticket is `OPEN` and unassigned.
- Resolution note: non-blank and required when resolving.

### Message

- Message: 1–5,000 characters after trimming.
- Employees may submit only `PUBLIC_COMMENT` messages.
- The author must be allowed to view the ticket.

The backend enforces all validation; client-side validation exists only to provide faster feedback.

## 11. Concurrency and consistency

- Map `tickets.version` with JPA optimistic locking.
- Return `409 Conflict` when a ticket was modified since the client loaded it.
- On conflict, the JavaFX client explains that the ticket changed and reloads the latest state.
- Taking an unassigned ticket must be atomic so that only one competing request succeeds.
- Assignment, status changes, resolution, cancellation, and reopening must each complete in a single transaction.

## 12. Automated testing

At minimum, automated tests must cover:

- Login with valid and invalid credentials.
- Inactive users cannot sign in or refresh a session.
- Employee creates a valid ticket.
- Employee edits permitted fields on their own open, unassigned ticket.
- Ticket editing rejects forbidden fields, stale versions, non-requesters, and tickets that are assigned or no longer open.
- Employee can view their own ticket but cannot discover or access another employee's ticket.
- Technician atomically takes an unassigned ticket.
- Manager assigns and reassigns a ticket.
- Technician adds an internal note; employee cannot receive it through either ticket or message endpoints.
- Technician resolves a ticket with a resolution note.
- Resolution without a note is rejected.
- Employee cancels an eligible ticket.
- Employee reopens a resolved ticket and the resolution state is cleared.
- Invalid status transitions are rejected.
- Concurrent ticket updates return a conflict.
- Role restrictions are enforced independently of the client.

## 13. MVP acceptance criteria

The MVP is complete when:

- All components build and run on Java 25.
- The version-controlled `schema.sql` creates a working SQLite database from an empty state, and Hibernate validates it at startup.
- Users can sign in, refresh their session, and sign out securely.
- Employees can submit, edit, view, comment on, cancel, and reopen their own tickets according to the workflow.
- Technicians can take, update, comment on, add internal notes to, and resolve tickets.
- Managers can view and assign all tickets and manage user accounts.
- Internal notes are visible only to technicians and managers.
- The JavaFX client communicates with the backend through the versioned REST API without blocking the UI thread.
- Backend authorization and validation protect every operation.
- Optimistic locking and atomic ticket-taking handle concurrent updates safely.
- The required automated tests pass.
- Out-of-scope features have not become dependencies for the MVP workflows.
