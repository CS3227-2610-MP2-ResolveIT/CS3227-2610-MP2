`src/resolveit/auth/LockoutPolicy.java` already has complete Javadoc for its public API:

- The `LockoutPolicy` class
- The `LockoutPolicy(int maxFailures)` constructor
- `isLockedOut(int failures)`
- `attemptsLeft(int failures)`

No changes were made because the requested documentation is already present and accurately describes the observable behavior.

Verification:

- `git status --short`: clean; no files changed.
- `./gradlew javadoc`: could not run because this workspace does not contain a `gradlew` script.
- `./gradlew compileJava`: could not run for the same reason.

The file’s existing Javadoc was reviewed semantically and conforms to the requested style.