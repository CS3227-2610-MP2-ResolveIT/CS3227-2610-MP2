Added short inline comments inside `pickTechnician` in [TechnicianRouter.java](</Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/negative-control-inline-comments/baseline/trial-01/workspace/src/resolveit/ticket/TechnicianRouter.java:7>) explaining:

- How the initial candidate and load baseline are established.
- Why technicians absent from `openCounts` receive a load of `0`.
- How the method retains the first technician with the fewest open tickets.

Validation passed:

- `git diff --check`
- `javac` compilation of `TechnicianRouter.java`