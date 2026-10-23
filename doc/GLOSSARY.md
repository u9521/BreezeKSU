# BreezeKSU

A trimmed fork of KernelSU. The goal is subtraction — keep only a clean kernel-level root solution — plus a small set of fork-owned features.

This glossary covers **fork-specific** concepts only. Upstream vocabulary (supercall, allowlist, app profile, and so on) lives in [FUNCTION_MAP.md](FUNCTION_MAP.md).

## Baseline

**upstream base** (`upstream_base`):
The upstream commit the fork replays its commits on top of, recorded in the anchor block of [PRUNING.md](PRUNING.md). It is a value, not a branch: `main` is the fork's landing branch and carries fork work as well, so the baseline is never read off a branch tip.
_Avoid_: freeze commit, frozen baseline, upstream version

**baseline advance**:
Moving `upstream_base` to a newer upstream commit and replaying the fork commits onto it. Only the base moves; behaviour does not.
_Avoid_: freeze, sync, upgrade

**re-freeze**:
Starting the fork over from a fresh upstream snapshot: replay the Breeze UI skin and the branding constants, and redo every pruning entry from scratch. Old tree tagged `freeze/<sha>`. Costs a whole pruning pass.
_Avoid_: freeze, re-baseline

**feature sync**:
Porting one upstream feature or fix into the fork as fork commits. The default mode; changes behaviour.
_Avoid_: update, merge, sync (bare)

**big sync**:
Advancing the baseline across many upstream commits at once. Human-triggered — never on a calendar.

## Pruning

**pruning table** ([PRUNING.md](PRUNING.md)):
The record of which upstream capabilities this fork removed or kept, and why. The **decision layer**: written by pruning sessions, only verified during sync.
_Avoid_: deletion list, log

**upstream function map** ([FUNCTION_MAP.md](FUNCTION_MAP.md)):
The record of what upstream contains, where it lives, and what it couples to. The **fact layer**, rewritten whenever upstream evolves.
_Avoid_: feature list, architecture doc

**residue list**:
Everything still referencing a pruned feature after its files are gone: ioctl numbers, feature ids, string keys, Manifest components, Breeze pages, CLI subcommands, module parameters, build workflows.
_Avoid_: impact surface, follow-up changes

**runtime off**:
Removing a feature by no longer registering its handler while keeping its ABI number reserved. The cheapest removal mechanism, and the correct one when coupling is unclear.

## Verification

**gate**:
A verification step run after a sync or a prune. Four of them: G1 pruning integrity, G2 UAPI agreement, G3 ksud compile, G4 Manager compile. Run by the `breeze-sync-gates` skill, because they are judgement calls with a checklist, not a script that returns green.
_Avoid_: check, CI step

**seam**:
A minimal fork edit inside an upstream-owned file (S1 in `MainActivity.kt`, S2 in the two settings screens, S4 in `WebUIActivity.kt`). A seam claim must be re-verified whenever upstream touches the containing function.
_Avoid_: patch point, injection point

## Sync bookkeeping

**ledger** ([sync/ledger.md](sync/ledger.md)):
One line per ported feature: upstream SHA set → fork commit SHA → feature name. Without it a later big sync cannot distinguish a ported feature from a missing one, and ports it twice.
_Avoid_: changelog, sync log

**port**:
Landing upstream code as a fork commit, carrying `Co-authored-by` trailers naming the upstream authors of that code. Attributing the work to whoever wrote it upstream is the point of the trailer, which is why no tool, assistant or session ever appears there.
_Avoid_: merge, cherry-pick (as a synonym for the whole act)
