# Conflict arbitration

## Priority (top to bottom, first hit wins)

1. **`protected_constants` matched** → keep the fork value, record it in the report.
2. **Seam file** (see `doc/seams.md`) → keep the seam, accept the rest of upstream's change.
3. **Inside `breezeui/`** → normally zero conflict (upstream does not touch it). On conflict, keep the fork version.
4. **Everything else** → decide by `triage-rules.md`: a must-port class gets a hand merge, a skip class takes ours.

## General fix extraction

A commit inside `removed_paths` is not skipped wholesale. Decide between three cases:

| Case | How to tell | Action |
| :--- | :--- | :--- |
| The fix only concerns a deleted file's internals | every hunk is inside a removed path | skip, record in the skipped list |
| The fix is cross-file and the deleted file is merely affected | hunks span removed and kept paths | hand-port the non-deleted parts |
| The fix touches UAPI or a shared structure | hits `uapi/` or a struct definition | **stop, ask a human** |

The third case in practice: a security fix that incidentally adjusts an `app_profile` struct field. The struct is UAPI, so the change ripples into ksud and the Manager.

## What not to do

- Do not accept an upstream reconstruction of a removed feature to make a conflict disappear. A remaining conflict is the better outcome.
- Do not "tidy up" fork changes while resolving a conflict.
- Do not auto-resolve a UAPI conflict.
- Do not re-add a feature by hand inside a sync commit; that is a separate decision with its own pruning entry.
