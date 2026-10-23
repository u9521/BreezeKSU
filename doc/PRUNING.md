# PRUNING.md — the fork's pruning index

> This file records only what the fork decided relative to upstream; it never accepts an upstream value.
> What upstream contains is in [FUNCTION_MAP.md](FUNCTION_MAP.md), which upstream evolution may rewrite.
> Why the two documents are separate: [decisions/README.md](decisions/README.md) §11.

This file is the **index and the anchor**. The detail for each decision lives in one file under [entries/](entries/), because a single table cannot hold what a pruning actually needs — which files moved, what each residue surface answered, how it was proven.

## Anchor block

Machine-checked by the `breeze-sync-gates` skill. Keep the keys; edit the values. Every entry must be reflected here: a path missing from `removed_paths` is a path the next sync will restore.

```yaml
uapi_anchor: 6                    # the KERNEL_SU_UAPI_VERSION this fork aligns with
upstream_base: 6d31a6137          # upstream baseline (v3.3.1-22); a value, not a branch tip
upstream_base_advanced: 2026-10-21   # last baseline advance; see "two words" below
residue_rev: 1                    # which revision of the residue surfaces the entries were written against
removed_paths: []                 # whole directories/files deleted; a matching upstream change is an error
removed_symbols: []               # deleted ioctls / feature ids (numbers reserved, never reused)
protected_constants:              # keep the fork value during sync; never accept upstream's
  - kernel/Kbuild::KSU_PACKAGE_NAME
  - kernel/Kbuild::KSU_EXPECTED_SIZE
  - kernel/Kbuild::KSU_EXPECTED_HASH
  - manager/app/build.gradle.kts::defaultManagerPackageName
  - manager/app/build.gradle.kts::defaultManagerName
```

## Residue surfaces

The checklist every pruning must answer, one verdict per surface. This list is the **single authority** — skills point at it rather than restating it, so a mechanism change is edited here once.

Bump `residue_rev` in the anchor block whenever a surface is added, removed or redefined, and update the G1 sweep commands in the `breeze-sync-gates` skill in the same change.

| # | Surface | What to check |
| :--- | :--- | :--- |
| 1 | ABI command numbers | ioctl / supercall numbers stay reserved; unregister the handler, never recycle the number |
| 2 | Feature ids | same rule; check `KSU_FEATURE_MAX` and the deliberately unused id |
| 3 | Strings | both `values/` and `values-zh-rCN/` |
| 4 | Manifest components / gradle deps | a component outliving its class stops the app building |
| 5 | Breeze pages | `breezeui/screen/<feature>/` — a `*Breeze.kt` plus `*Entry.kt` pair |
| 6 | supercall wrappers | `ksucalls.rs` and the JNI bridge mirror the kernel side |
| 7 | ksud CLI | the subcommand that now has no handler |
| 8 | module parameters | bootconfig keys read at load time — stop writing them before deleting the read |
| 9 | Build workflows | embedded assets and their CI steps |
| 10 | Seams | does anything in [seams.md](seams.md) depend on a symbol being removed? |

### When this list changes

Three triggers, any one obliging a revision:

1. A pruning finds a reference surface the list did not cover.
2. **G1 misses something** — upstream brought a feature back and the sweep did not catch it. This is the only evidence that can falsify the list, so it is the strongest trigger.
3. Upstream replaces a mechanism the list names (a numbering scheme retired, a registry moved).

A revision updates the surface table, the `residue_rev` value, and the G1 commands together. Entries written before the revision keep their `residue-rev` and are read against it; they are never rewritten, because an entry records what was true when the work was done.

## Entries

One file per decision, named `<ID>-<slug>.md` where `<ID>` is the FUNCTION_MAP identifier (`M5`, `U4.2`) or `F<n>` for a fork-owned feature with no upstream counterpart.

Each entry carries six sections — `Why`, `Surface`, `Residue`, `Verified`, `Sync risk`, `Rollback` — plus front-matter with `status`, `mechanism` and `residue-rev`. The template is [entries/TEMPLATE.md](entries/TEMPLATE.md).

| ID | Status | Mechanism | Entry | Why |
| :--- | :--- | :--- | :--- | :--- |
| — | — | — | — | *(no pruning or addition recorded yet)* |

## Two words: baseline advance vs re-freeze

The two actions differ in cost by an order of magnitude, and both have been called "freeze" historically. Keep them apart.

| | **baseline advance** | **re-freeze** |
| :--- | :--- | :--- |
| What | move `upstream_base` to a newer upstream commit, replay fork commits on the new base | take a fresh upstream snapshot as the new base, **replay the Breeze UI, the branding, and every pruning** |
| How | rebase the fork commits onto the new tip | swap trees, replay, tag the old tree `freeze/<old base sha>` |
| Precondition | upstream delta is small, does not touch pruned areas, fork commit count is low | any of the four triggers in [decisions/README.md](decisions/README.md) §6 |
| Cost | minutes | a full pruning pass |
| Anchor change | `upstream_base` only | `upstream_base` plus a tag plus archiving the old tree |

**Re-freeze triggers** ([decisions/README.md](decisions/README.md) §6, any one suffices): UAPI behind by ≥2, more than 20 conflicting files in one sync, KMI generation break, toolchain generation break. When none fires, calling a baseline advance a re-freeze is overreaction.

Most recent: **2026-10-21 baseline advance** `469c4282e` → `6d31a6137` (2 upstream commits, no overlap with the fork; all fork commits `range-diff` as `=`). No tag, no pruning redone.

## Rules that bind a sync

1. If an incoming upstream diff touches a path in `removed_paths`, **stop and report**: a human decides.
2. If an upstream change hits a `protected_constants` entry, keep the fork value and record it in the sync report.
3. A pruned feature's **Breeze page** belongs in its entry's residue table and is deleted in the same change.
4. A sync session **reads and verifies** this table and its entries; it never edits a decision.
5. Sync completion includes "FUNCTION_MAP.md and PRUNING.md both updated".

## Rules that bind a prune or an addition

1. Every entry answers **all ten surfaces**, even when the answer is "not applicable".
2. An entry is written **before** the work starts as a plan, and completed once the work is verified. It is never deleted: upstream can bring a feature back, and the entry is what the next sync reads.
3. Completing an entry means three writes in one change: the entry file, the index row above, and the anchor block (`removed_paths` / `removed_symbols` for prunes).
4. If an upstream file was modified, the same change adds or updates its row in [seams.md](seams.md).
