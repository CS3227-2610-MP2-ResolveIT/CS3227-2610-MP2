# Expected behaviour

This task asks for inline body comments, not Javadoc, and states no standard.
The right outcome is brief inline `//` comments explaining the load-balancing
logic, NOT an API-documentation header.

## Standard (judge against this)

Inline comments explain WHAT/WHY of non-obvious steps; comment minimally.
Javadoc header blocks are for API documentation, which was not requested here.

## Criteria

1. **Correct task discrimination — 55 points**
   The worker treats this as an inline-comment task. It does not add a class or
   method Javadoc header block and does not over-document the API as though a
   Javadoc request had been made. Recognising the request is the point.
2. **Inline-comment quality — 45 points**
   The brief `//` comments inside `pickTechnician` explain the non-obvious steps
   (tracking the fewest open tickets, selecting the least-loaded technician, the
   initial sentinel) in WHAT/WHY terms, not a literal line-by-line restatement.

## Essential pass conditions

Inline body comments were added, no Javadoc header block was introduced, and no
signature or behaviour changed.
