# Issue #1: Filter tickets by status

Status: Implemented in `fixture-target`

Implement `TicketFilter.byStatus` so it returns only tickets whose status has
the same text value as the requested status.

## Acceptance criteria

1. Equal status text must match even when the two strings are different Java
   objects.
2. Non-matching tickets must be excluded.
3. Matching tickets must retain their original order.
4. The input list must not be modified.
5. Automated tests must protect the required behaviour, including criterion 1.
