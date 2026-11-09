---
name: breeze-prune
description: Remove an upstream KernelSU feature from the BreezeKSU fork, or add a fork-owned one, without leaving dangling references. Use when asked to prune, trim, drop or remove a feature, to shrink the manager/kernel/ksud surface, to add a fork-only feature, or to work through an entry of doc/FUNCTION_MAP.md.
disable-model-invocation: true
---

# Prune or add a feature

The fork's main work is subtraction, with additions as the other half. Both are **surgical** changes with a residue sweep — not a delete-and-compile-until-green loop.

Every change gets one entry under `doc/entries/`, written in the same change.

## Before starting

Read `doc/FUNCTION_MAP.md` at the target ID (K/U/M numbering), the anchor block and residue surface list in `doc/PRUNING.md`, and `doc/seams.md`.

Completion: you can name the target ID, its files, everything FUNCTION_MAP lists as coupled to it, and which residue surfaces you expect to hit.

## 1. Decide the mechanism

For a prune, pick one and write it down:

- **Block delete** — the feature is a directory or a self-contained set of files.
- **Surgery** — the feature is spread across shared files; remove its branches only.
- **Runtime off** — keep the code, stop registering the handler. Cheapest, and right when coupling is unclear.

For an addition: **new files** where possible, **minimal delta** to an upstream file where necessary.

Runtime-off is a legitimate end state. Do not escalate to surgery just because deleting feels more complete.

Completion: mechanism chosen with a one-sentence reason.

## 2. Enumerate the residue

Answer **every surface** in the residue list ([PRUNING.md](../../../doc/PRUNING.md#residue-surfaces)) — "not applicable" is a valid verdict, a skipped surface is not. The list lives there rather than here so a mechanism change is edited once, not in every copy.

Completion: every surface has an explicit verdict, recorded as a table.

## 3. Cross-check the seams

Walk `doc/seams.md`. For each row, ask whether it depends on something this change removes or redefines. Use the `Depends on` column rather than re-reading each file:

```bash
# for each symbol S being removed:
rg -n "$S" <path from the inventory>
```

A hit is a seam that breaks. It goes into the residue table under surface 10 with the fix spelled out — usually "drop that import and its route", occasionally "relocate the seam". Leaving it to the compiler only catches the Kotlin half: a `Kbuild` constant or a `strings` key fails silently.

Known live examples: `MainActivity.kt`'s seam imports the module-repo, script-run and colour-palette screens; the two settings seams carry the template entry; the Breeze settings entry wires the sulog toggle.

Completion: every inventory row has an "affected / not affected" verdict, and each affected row has its fix named.

## 4. Do it

Work outward from the leaves: the UI page, then the data layer, then the CLI, then the JNI wrapper, then the kernel handler. Deleting the kernel side first hides which userspace code still expects it.

Parameters and ABI numbers are the exception — see the ordering rules in [kernel](../../architecture/kernel.md).

Completion: the feature has no reachable entry point (prune), or the new behaviour is reachable and exercised (addition).

## 5. Verify

Run `breeze-sync-gates`: G1 checks that what you just removed stays removed, including the seam dependencies; G2 that the three layers still agree; G3 and G4 that the remainder builds.

Completion: gates green, or stopped at the first red with its cause identified.

## 6. Record

Three writes, one change:

1. **The entry** — `doc/entries/<ID>-<slug>.md`, copied from [the template](../../../doc/entries/TEMPLATE.md). Six sections, front-matter with `status`, `mechanism` and the `residue-rev` you worked against.
2. **The index** — one row in the table in `doc/PRUNING.md`.
3. **The anchor block** — `removed_paths` / `removed_symbols` for a prune. G1 reads them, and an entry missing from the anchor is an entry the next sync will restore.

If an upstream file was modified — including relocating a seam — update its row in `doc/seams.md` in the same change.

Completion: entry, index row, anchor block and seam inventory all describe the same change.

## Stop rules

- A parameter or ABI number would have to be recycled: stop, that is never the fix.
- Residue includes a `protected_constants` entry: stop and ask.
- The feature turns out to be load-bearing (modules stop booting, install breaks): restore and report rather than patching around it.
