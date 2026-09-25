Added Javadoc to the public API of both ticket classes:

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-01/workspace/src/resolveit/ticket/Ticket.java)
  - Documented the record and all three components: `id`, `subject`, and `countsAgainstQuota`.

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-01/workspace/src/resolveit/ticket/TicketQuota.java)
  - Documented the class, constructor, `remaining`, `assertCanCreate`, and `getDailyLimit`.
  - Included relevant return behavior, quota-counting semantics, and the `QuotaExceededException` condition.

Verification completed successfully:

- `git diff --check`
- Direct compilation of all ticket package classes with `javac`