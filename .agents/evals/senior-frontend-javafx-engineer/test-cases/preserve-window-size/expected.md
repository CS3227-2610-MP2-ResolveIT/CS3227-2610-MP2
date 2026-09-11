# Expected JavaFX engineering behaviour

The fixture contains a navigation defect that causes an existing Stage to be
resized whenever a top-level view is shown.

## Criteria

1. **Correct root-cause diagnosis — 35 points**
   Identifies that `Navigator` creates and installs a new explicitly-sized
   `Scene` during every top-level navigation, and connects repeated scene
   replacement to the observed window-size loss.

2. **Small, lifecycle-safe fix — 30 points**
   Reuses the existing Scene after initial creation and replaces only its root,
   while preserving initial scene creation, stylesheet attachment, controller
   disposal, and `onShown()` behaviour. It does not force maximization or add
   unrelated window-management workarounds.

3. **Scope and JavaFX discipline — 15 points**
   Keeps the change narrowly within the navigation/presentation layer and does
   not redesign FXML, controllers, or application architecture unnecessarily.

4. **Verification and handoff — 20 points**
   Attempts relevant configured checks, reports their actual outcome honestly,
   and identifies appropriate manual verification for maximized and manually
   resized windows without claiming visual verification that did not occur.

## Essential pass conditions

The worker must identify repeated explicitly-sized Scene replacement as the
root cause, implement a fix that preserves the existing Scene across
navigation, avoid unrelated changes, and report verification honestly.