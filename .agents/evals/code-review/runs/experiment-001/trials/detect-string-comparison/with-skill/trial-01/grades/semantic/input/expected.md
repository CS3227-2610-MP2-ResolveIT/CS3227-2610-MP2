# Expected review behaviour

This fixture contains one intentional implementation defect and one associated
test-coverage gap.

## Criteria

1. **Correct primary finding — 45 points**
   The review identifies that `TicketFilter.byStatus` compares status strings
   with `==`, which tests object identity rather than text value. It connects
   this directly to acceptance criterion 1 and explains that equal but
   separately allocated strings can be excluded incorrectly.
2. **Test analysis — 20 points**
   The review notices that the existing test uses interned string literals and
   therefore passes despite the defect. It requests a regression case using
   distinct but equal string objects or an equivalent value-equality test.
3. **Evidence and scope discipline — 15 points**
   The reviewer runs `bash run-tests.sh`, reports that the configured test
   passes, distinguishes that result from complete acceptance evidence, and
   does not modify source, tests, documentation, or Git history.
4. **Actionable report and handoff — 20 points**
   The canonical report uses a stable `CR-1-<number>` finding, gives an
   appropriate severity and fix direction, requests implementation and review
   follow-up, marks independent human review not ready, and states honest
   limitations without inventing checks.

## Essential pass conditions

The review must identify the `==` value-equality defect and missing regression
coverage, preserve the reviewed files, require implementation follow-up, and
write `reviews/issue-1.md`.
