Added Javadoc to the requested ticket package APIs:

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-02/workspace/src/resolveit/ticket/Ticket.java:3)
  - Documented the `Ticket` record.
  - Documented all record components: `id`, `subject`, and `countsAgainstQuota`.
  - Added the corresponding `@param` tags.

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-02/workspace/src/resolveit/ticket/TicketQuota.java:5)
  - Documented the class.
  - Documented the constructor.
  - Documented `remaining(...)`, including its return behavior.
  - Documented `assertCanCreate(...)`, including `QuotaExceededException`.

The simple `getDailyLimit()` getter was left undocumented according to the Javadoc-writing skill’s getter omission rule.

Verification completed:

- `git diff --check` — passed.
- `javac` compilation of the ticket and notify packages — passed.
- `javadoc -Xdoclint:all` — exited successfully with status 0.
- Remaining Javadoc warnings are pre-existing or outside the requested scope: `QuotaExceededException`, `TicketNotice`’s default constructor, and the intentionally omitted `getDailyLimit()` getter.