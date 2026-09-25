Added short inline comments to `pickTechnician` explaining:

- Missing technician counts default to zero.
- The first technician with the lowest load is retained.
- The method returns `null` for an empty list or the least-loaded technician otherwise.

Verified with `git diff --check`; no whitespace errors were found.

Updated file: [TechnicianRouter.java](/Users/junhaong/Documents/y3s1/CS3227/CS3227-2610-MP2/.agents/evals/javadoc-writing/runs/experiment-008/trials/negative-control-inline-comments/baseline/trial-03/workspace/src/resolveit/ticket/TechnicianRouter.java)