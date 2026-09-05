# Review criteria

Use the relevant sections; do not demand every principle mechanically.

## Correctness and acceptance

- Map each changed requirement to code, tests/evidence, and user outcome.
- Check happy path, invalid inputs, boundaries, errors, state transitions,
  persistence/recovery, adjacent features, and unauthorised behaviour.
- Test realistic user workflows and record exact expected versus observed
  results. GUI smoke tests prove only exercised paths.

## Code and design

- readability, naming, long methods, nesting, expressions, magic values;
- Java conventions: package/class/method/variable naming, 4-space indentation,
  Java-source line length, consistent imports, braces, scope, and useful
  Javadocs. Apply resource-formatting rules only when the project defines them
  for that resource type;
- SLAP, KISS, abstraction, happy-path clarity, dead code, scope, duplication;
- comments, exceptions, assertions, logging, defensive coding;
- responsibility, cohesion, coupling, separation of concerns, dependency
  direction, testability, and UI/domain/persistence boundaries; and
- security/privacy risks where relevant.

## Tests and regression

- suitable unit/integration/system/acceptance level;
- meaningful assertions, isolation, determinism, and no implementation-copying;
- positive/negative cases, equivalence partitions, boundaries, state changes,
  persistence, integration, and regression protection.

## Documentation consistency

Compare code and observed behaviour with the User Guide, Developer Guide,
manual-testing appendix, commands, examples, diagrams, error claims, feature
status, and release status. Classify each affected claim as verified, stale,
inaccurate, incomplete, or not verifiable. Do not edit; route to the
documentation workflow for follow-up.

These criteria follow the CS2103/T guidance on [code quality], [quality
assurance], [testing], [test design], [architecture], [principles],
[refactoring], [debugging], [security], and the [SE-EDU Java coding standard].

[code quality]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/codeQuality.html
[quality assurance]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/qualityAssurance.html
[testing]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/testing.html
[test design]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/testCaseDesign.html
[architecture]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/architecture.html
[principles]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/principles.html
[refactoring]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/refactoring.html
[debugging]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/debugging.html
[security]: https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/security.html
[SE-EDU Java coding standard]: https://se-education.org/guides/conventions/java/
