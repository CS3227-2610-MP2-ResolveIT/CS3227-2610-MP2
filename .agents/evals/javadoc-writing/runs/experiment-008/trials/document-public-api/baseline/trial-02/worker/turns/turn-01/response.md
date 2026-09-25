Added Javadoc to the public API of both ticket classes:

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-02/workspace/src/resolveit/ticket/TicketQuota.java)
  - Documented the class and quota-counting behavior.
  - Documented the constructor, `remaining`, `assertCanCreate`, and `getDailyLimit`.
  - Added parameter, return-value, and exception documentation.

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-02/workspace/src/resolveit/ticket/Ticket.java)
  - Documented the record and all record components.

Validation completed successfully:

- `git diff --check`
- `javac` compilation of the ticket package
- Javadoc generation

Javadoc generation reported only pre-existing warnings for the undocumented `QuotaExceededException`, which was outside the requested files.