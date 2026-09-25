Added short inline comments to `pickTechnician` in [TechnicianRouter.java](</Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/negative-control-inline-comments/baseline/trial-02/workspace/src/resolveit/ticket/TechnicianRouter.java:7>) explaining:

- Tracking the least-loaded technician.
- Treating missing map entries as zero open tickets.
- Preserving the first technician when loads are tied.

Validation completed successfully:

- `git diff --check`
- `javac` compilation of `TechnicianRouter.java`