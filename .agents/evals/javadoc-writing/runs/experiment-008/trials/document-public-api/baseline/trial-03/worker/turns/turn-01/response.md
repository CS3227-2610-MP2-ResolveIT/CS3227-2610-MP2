Added Javadoc to the public API in:

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-03/workspace/src/resolveit/ticket/TicketQuota.java)
  - Class description
  - Constructor parameter
  - `remaining` behavior, including quota-counted tickets and non-negative results
  - `assertCanCreate` behavior and thrown exception
  - `getDailyLimit` return value

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/baseline/trial-03/workspace/src/resolveit/ticket/Ticket.java)
  - Record description
  - Documentation for all record components

Validation completed successfully:

- `javac` compilation passed.
- Javadoc generation with full doclint passed. It reported only pre-existing missing documentation warnings for the untouched `QuotaExceededException.java`.