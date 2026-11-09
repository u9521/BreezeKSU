# Sync report — 2026-10-27 baseline advance

`6d31a6137` → `75e179d3` (v3.3.1-22 → v3.3.1-25). Mode: **baseline advance**, not a re-freeze — none of the four triggers in [decisions/README.md](../../decisions/README.md) §6 fired: UAPI unchanged at 6, three commits touching five files, no KMI or toolchain movement.

## Commits

| Upstream | Subject | Paths | Route |
| :--- | :--- | :--- | :--- |
| `3352209f4` | userspace: Update waitsys to fix crash | `ksud/bin/{aarch64,riscv64,x86_64}/waitsys` | ported (opaque binaries, no in-tree source to review) |
| `528bafc56` | kernel: remove useless code (#3945) | `kernel/feature/selinux_hide.c` | ported — drops a `mutex_lock`/`unlock` pair around a plain pointer read; `fake_status` is written under the same lock in `initialize_fake_status()`, so the pair bought nothing |
| `75e179d37` | update ads.txt | `website/docs/public/ads.txt` | ported — see "Needs human" |

Nothing hit `removed_paths`, `uapi/`, `protected_constants` or any [seams.md](../../seams.md) row. The fork's only delta under `kernel/`, `userspace/` or `website/` is `kernel/Kbuild`, which none of these commits touch.

## How it landed

```
git fetch upstream --prune --tags
git rebase --onto 75e179d3 6d31a6137 dev
```

Five fork commits replayed with no conflict; `git range-diff 6d31a6137..3e3f1fc71 75e179d3..daa0fc519` reports `=` for all five. No manual port, so [ledger.md](../ledger.md) gains no row — it records features ported by hand, and a rebase carries none.

## Anchors

- `doc/PRUNING.md`: `upstream_base` → `75e179d3`, `upstream_base_advanced` → `2026-10-27`.
- `doc/FUNCTION_MAP.md`: baseline line → `v3.3.1-25-g75e179d37`; K5.5 line count 1257 → 1254, the only C file whose size moved.
- `doc/HANDOFF.md`: baseline row.
- `removed_paths`, `removed_symbols`, `uapi_anchor`, `residue_rev`: untouched — no pruning decision changed.

## Gates

| Gate | Result |
| :--- | :--- |
| Spec gates (`verify-all`) | green — 9/9 |
| G1 pruning integrity | green — every `removed_paths` entry still absent, no `removed_symbols` |
| G2 UAPI agreement | green — `KERNEL_SU_UAPI_VERSION = 6` at both bases, equal to `uapi_anchor`; `KSU_FEATURE_MAX` unchanged |
| G3 ksud Android compile | skipped — no Rust source changed; the only `userspace/` delta is three embedded binaries |
| G4 Manager compile | skipped — no Kotlin, gradle or asset change |

## Needs human

1. **`ads.txt` now names upstream's AdSense account** (`pub-2610070972052494`, was `pub-8356785667482909`). `deploy-website.yml` publishes `website/` from `main`, so the fork's docs site would serve an ad slot paying upstream. Either keep it deliberately, replace the id, or stop shipping the file — a decision for `PRUNING.md`, not for a sync.
2. **No report exists for the 2026-10-21 advance.** Not backfilled; the anchor block and git history are the record.

## Not done here

`dev` only. `main` takes this with the next verified batch, and nothing was pushed.
