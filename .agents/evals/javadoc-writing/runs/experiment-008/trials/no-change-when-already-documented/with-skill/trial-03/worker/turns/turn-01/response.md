`src/resolveit/auth/LockoutPolicy.java` already contains complete Javadoc for its public API:

- `LockoutPolicy` class
- `LockoutPolicy(int maxFailures)` constructor
- `isLockedOut(int failures)`
- `attemptsLeft(int failures)`

No changes were made because the existing documentation already satisfies the Javadoc-writing skill’s requirements and accurately describes the code’s behavior.

Verification:

- `javadoc -quiet -d ... src/resolveit/auth/LockoutPolicy.java` — passed, exit 0
- `javac -d ... src/resolveit/auth/LockoutPolicy.java` — passed, exit 0
- `git diff --check` — passed, exit 0
- Working tree — unchanged

The requested Gradle checks could not be run because this workspace does not contain `./gradlew` or any Gradle build configuration.