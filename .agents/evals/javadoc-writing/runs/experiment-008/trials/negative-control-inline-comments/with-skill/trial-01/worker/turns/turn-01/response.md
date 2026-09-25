Added three short inline comments to [`TechnicianRouter.java`](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/negative-control-inline-comments/with-skill/trial-01/workspace/src/resolveit/ticket/TechnicianRouter.java):

- Explains that `chosen` tracks the least-loaded technician found so far.
- Documents that missing entries in `openCounts` default to zero.
- Explains that strict comparison preserves the earlier technician when loads tie.

Verification:

- `git diff --check` passed.
- No Gradle wrapper is present, so Java compilation and Javadoc checks were unavailable.