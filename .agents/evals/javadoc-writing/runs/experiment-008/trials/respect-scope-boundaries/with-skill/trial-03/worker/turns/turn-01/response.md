Added Javadoc to [src/resolveit/access/AccessPolicy.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/respect-scope-boundaries/with-skill/trial-03/workspace/src/resolveit/access/AccessPolicy.java).

Documented:

- `AccessPolicy` and its lockout policy.
- `AccessPolicy(int maxAttempts)`.
- `isLockedOut(int failedAttempts)`.
- `remainingAttempts(int failedAttempts)`.

The private implementation field was left undocumented, and no other files were changed.

Verification:

- `git diff --check` — passed.
- `./gradlew :app:javadoc` — could not run; the workspace has no Gradle wrapper (`exit 127`).
- `./gradlew :app:compileJava` — could not run; the workspace has no Gradle wrapper (`exit 127`).
- Direct `javadoc -Xdoclint:all` — passed (`exit 0`).
- Direct `javac` compilation — passed (`exit 0`).

