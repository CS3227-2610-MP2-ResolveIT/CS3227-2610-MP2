Fixed `src/main/java/resolveit/ui/Navigator.java`.

Navigation recreated a fixed 1120 × 720 scene each time. It now creates the scene once and replaces only its root afterward, preserving window sizing.

Initial dimensions, shared stylesheet, outgoing controller disposal, and `stage.show()` → `onShown()` ordering remain intact.

Verification:
- `git diff --check` passed.
- Compilation was blocked because `JAVAFX_LIB` is unset.
- GUI behavior remains unverified; this repository slice has no application launcher or GUI tests.