`src/resolveit/auth/LockoutPolicy.java` already contains complete Javadoc for its public API:

- `LockoutPolicy` class
- `LockoutPolicy(int maxFailures)` constructor
- `isLockedOut(int failures)`
- `attemptsLeft(int failures)`

The existing documentation includes appropriate summaries, `@param` tags, and `@return` tags. No changes were made, preserving the already-correct file.

Verification:

- `git diff --check` — passed.
- Working tree — clean; no files modified.
- `./gradlew javadoc` — could not run because this isolated workspace has no `gradlew` wrapper or Gradle build files.
- `./gradlew compileJava` — could not run for the same reason.