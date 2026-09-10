# Project checks

Discover before running. Use only existing, non-mutating commands. Classify
each as `Passed`, `Failed`, `Not configured`, `Not applicable`, or `Blocked`.

Inspect Gradle tasks and configured plugins, then run applicable commands such
as:

```text
./gradlew tasks
./gradlew test
./gradlew check
./gradlew build
<existing project smoke/resource/packaging task>
```

Do not install plugins, dependencies, JDKs, or analysis tools. Do not assume
Checkstyle, PMD, SpotBugs, JaCoCo, Error Prone, or `src/test` exists. Record
unavailable checks as `Not configured` or `Blocked`, never as passed.

Report command, exit result, relevant output, and limitations.
