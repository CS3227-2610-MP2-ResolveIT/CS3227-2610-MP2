Added Javadoc to the public API in:

- [TicketQuota.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-03/workspace/src/resolveit/ticket/TicketQuota.java)
  - Class description
  - Constructor and `dailyLimit` contract
  - `remaining(...)` behavior and return value
  - `assertCanCreate(...)` behavior and `QuotaExceededException`

- [Ticket.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/document-public-api/with-skill/trial-03/workspace/src/resolveit/ticket/Ticket.java)
  - Record description
  - Documentation for all three record components

Verification:

- `git diff --check` — passed.
- `./gradlew :<module>:javadoc` — not available; this fixture does not contain a Gradle wrapper or build module.
- `./gradlew :<module>:compileJava` — not available for the same reason.
- Equivalent direct `javadoc` invocation — passed with exit status `0`.
- Equivalent direct `javac` compilation — passed with exit status `0`.

The direct Javadoc run reported three pre-existing warnings for the undocumented `QuotaExceededException` class/constructor and `TicketQuota.getDailyLimit()` getter. The getter was intentionally left undocumented because the Javadoc-writing standard excludes simple getters.