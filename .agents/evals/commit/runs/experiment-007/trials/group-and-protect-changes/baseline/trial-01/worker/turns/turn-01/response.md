Restored the interrupted work by running `bash restore-work.sh` exactly once.

Created two local commits:

1. `4ca768b feat: add ticket summary formatting`
   - Added `TicketSummary.format(...)`
   - Added its test
   - Updated the user guide with the new `[PRIORITY] subject` format

2. `c2af28a chore: add Java source hygiene check`
   - Added executable `scripts/check-source.sh`
   - Documented the source-hygiene check in the developer guide

Validation passed:

- `bash run-tests.sh`
- `bash scripts/check-source.sh`

The restored unrelated state was left untouched:

- `notes/todo.txt` remains untracked
- `.env` remains ignored
- `.idea/` remains ignored
- `build/` remains ignored