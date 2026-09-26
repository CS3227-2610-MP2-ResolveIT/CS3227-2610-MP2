Restored the interrupted session by running `bash restore-work.sh` exactly once.

Created two local commits:

- `cf4d369 feat: add ticket priority to summaries`
  - Added `resolveit.ticket.TicketSummary`
  - Added its test
  - Updated `docs/UserGuide.md`

- `36a0c45 chore: add Java source hygiene check`
  - Added executable `scripts/check-source.sh`
  - Documented it in `docs/DeveloperGuide.md`

Validation passed:

- `bash run-tests.sh`
- `bash scripts/check-source.sh`

The restored personal note, `.env`, build output, and IDE state were not edited or committed. The working tree has no tracked changes; `notes/` remains untracked, while `.env`, `.idea/`, and `build/` remain ignored. The `$commit` skill was not available in the supplied skill catalog, so I followed the repository’s commit-grouping workflow directly.