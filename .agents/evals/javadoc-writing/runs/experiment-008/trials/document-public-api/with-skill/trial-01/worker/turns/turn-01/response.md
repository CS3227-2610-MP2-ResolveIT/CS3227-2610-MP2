Added Javadoc to the public ticket API in:

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-01/workspace/src/resolveit/ticket/TicketQuota.java)
  - Class description
  - Constructor
  - `remaining(...)`
  - `assertCanCreate(...)`
  - `getDailyLimit()`

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-01/workspace/src/resolveit/ticket/Ticket.java)
  - Record description
  - Documentation for all three record components

Verification:

- `git diff --check` — passed.
- Direct `javadoc` generation for the ticket classes — passed with two pre-existing warnings from the out-of-scope `QuotaExceededException.java`, which has no Javadoc.
- `javac` compilation of the ticket classes — passed.
- `./gradlew :<module>:javadoc` and `./gradlew :<module>:compileJava` — unavailable because this workspace has no Gradle wrapper or build configuration.