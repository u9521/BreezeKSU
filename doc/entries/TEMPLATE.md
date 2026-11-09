---
status: prune | add | modify
mechanism: block delete | surgery | swap | runtime off | new files | minimal delta
residue-rev: 1
---

# <ID> — <short name>

One line: what this entry covers, and the upstream identifier it maps to.

## Why

Why this change is worth making, in one paragraph. Written for whoever reads it in six months and has to decide whether it is still wanted.

## Surface

Every file the change touches. For a prune, what was removed. For an addition, what was created. Group by layer, because a reader is usually chasing one layer at a time.

```
kernel/           — <files or "none">
userspace/ksud/   — <files or "none">
manager/          — <files or "none">  (include the Breeze page if one goes)
uapi/             — <files or "none">
build / CI        — <files or "none">
```

## Residue

One row per surface from the list in [PRUNING.md](../PRUNING.md#residue-surfaces). "Not applicable" is a valid verdict; a blank row is not. This table is what a reviewer checks, so the verdicts must be specific enough to verify.

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | | |
| 2 Feature ids | | |
| 3 Strings | | |
| 4 Manifest / gradle | | |
| 5 Breeze pages | | |
| 6 supercall wrappers | | |
| 7 ksud CLI | | |
| 8 Module parameters | | |
| 9 Build workflows | | |
| 10 Seams | | |

## Verified

What was actually run, and what it returned. Not "should be fine" — the command and its result. For a prune this is normally the `breeze-sync-gates` run plus any device check; for an addition it is the compile and the manual exercise of the new behaviour.

## Sync risk

What happens if upstream touches this area later. Name the files or symbols that would collide, and what a future sync should watch for. An entry with nothing here is either trivial or was not thought through.

## Rollback

How to undo this change if it turns out wrong. Usually a commit range and a note on what data or ABI state cannot be restored — a pruned feature whose ABI numbers were reserved comes back at the same numbers, never new ones.
