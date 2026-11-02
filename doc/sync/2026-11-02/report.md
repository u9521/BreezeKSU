# Sync report — 2026-11-02 baseline advance

`b1fce49e` → `28db74f6` (v3.3.1-34 → v3.3.1-36). Mode: **baseline advance**, not a re-freeze — none of the four triggers in [decisions/README.md](../../decisions/README.md) §6 fired: UAPI unchanged at 6, two upstream commits touching eight `kernel/` files, no KMI or toolchain movement.

## Commits

| Upstream | Subject | Paths | Route |
| :--- | :--- | :--- | :--- |
| `bec9c1347` | kernel: use vmlinux.btf query to workaround 6.6.144's ABI changes (#3962) | `kernel/Kbuild`, `kernel/core/init.c`, new `kernel/infra/btf_parser.{c,h}`, new `kernel/selinux/inode.c`, `kernel/infra/file_wrapper.c`, `kernel/selinux/selinux.h` | inherited — the fork never touched any of these files, so the port is the upstream text verbatim. 6.6.144 inserted `lbs_backing_file` before `lbs_inode` in `lsm_blob_sizes`, which invalidates every `selinux_inode()` call site; the fix reads the live offset out of `vmlinux.btf` at `__init` time instead of trusting a compiled-in constant |
| `28db74f6` | kernel: Fix SELinux generation tracking for policy updates (#3964) | `kernel/selinux/rules.c` | inherited — `reset_avc_cache()` now takes the new policy's sequence number instead of passing 0, and both call sites set `pol->latest_granting = old_pol->latest_granting + 1` before the `rcu_assign_pointer`. The fork had no edit in this file |

Path intersection with the fork's delta is empty: the fork's changed paths are `manager/`, `doc/`, the agent-framework tree, `js/`, `website/` and `kernel/Kbuild`, and the incoming diff is eight `kernel/` files of which only `Kbuild` is shared. There the two sides touch disjoint regions — upstream adds `infra/btf_parser.o` and `selinux/inode.o` around line 40, the fork sets `KSU_PACKAGE_NAME`/`KSU_EXPECTED_SIZE`/`KSU_EXPECTED_HASH` around line 173 — so the rebase replayed all three fork commits without a single conflict.

## How it landed

```
git fetch upstream --prune --tags
git rebase --onto 28db74f6 b1fce49e dev
```

`b1fce49e` → `28db74f6` carried `dev` from `f4abf06d4` to `699a23828`:

| Before | After | Range-diff |
| :--- | :--- | :--- |
| `b1cfc4732` manager: rebuild the Manager on a five-layer Breeze-only architecture | `c32168bce` | `=` |
| `a8efbdb3c` docs: advance the upstream baseline to b1fce49e | `d83bebff3` | `=` |
| `f4abf06d4` manager: follow the template's build, signing and overlay blur | `699a23828` | `=` |

All three are `=`, so no manual port and [ledger.md](../ledger.md) gains no row — the ledger records features ported by hand, and a rebase carries none.

The `protected_constants` were re-read after the rebase rather than assumed:

```
kernel/Kbuild:173  KSU_PACKAGE_NAME ?= u9521.breezeksu
kernel/Kbuild:177  KSU_EXPECTED_SIZE ?= 0x01ed
kernel/Kbuild:178  KSU_EXPECTED_HASH ?= 3ebb1bb93ce21987d5c467211d0617b9ce03565fb17acaa865d56dfd4cd395ee
```

`manager/app/build.gradle.kts:48` still resolves `defaultManagerPackageName` to `u9521.breezeksu`, and upstream's two new `kernelsu-objs` lines are present at `kernel/Kbuild:43-44`.

## Anchors

- `doc/PRUNING.md`: `upstream_base` → `28db74f6`, `upstream_base_advanced` → `2026-11-02`; the "Most recent" line names this advance and the previous one moves to "Previous".
- `doc/FUNCTION_MAP.md`: baseline line → `v3.3.1-36-g28db74f6f`; two line references refreshed against the new tip — K2.10 `595` → `600` (`file_wrapper.c` gained the BTF lookup and the `IS_ERR` branch), K1.1 `:207` → `:218` (`init.c` gained the `ret` variable and the `ksu_selinux_inode_init()` call). K2.4/K2.5/K2.6/K2.7/K2.13/K4.2/K4.3 were re-measured and are unchanged.
- `doc/HANDOFF.md`: Baseline row → `28db74f6` (v3.3.1-36).
- `removed_paths`, `removed_symbols`, `uapi_anchor`, `residue_rev`, `protected_constants`: untouched — no pruning decision changed and `uapi/` is outside the incoming diff.

## Coupling worth naming

`selinux_inode()` gained a fork-visible replacement. Upstream's own sweep covers its two call sites (`file_wrapper.c`, and the `__init` probe in `inode.c`), and the fork has no third: `kernel/feature/selinux_hide.c` works on `selinux_policy` and SIDs, never on `inode_security_struct`. A future fork patch that reaches for `selinux_inode()` directly would silently reintroduce the 6.6.144 crash, so it should call `ksu_selinux_inode()` and check `IS_ERR`.

## Gates

| Gate | Result |
| :--- | :--- |
| Spec gates (`verify-all`) | green — see the batch's own run |
| G1 pruning integrity | green — every `removed_paths` entry still absent, `protected_constants` hold the fork values (re-read, not inferred) |
| G2 UAPI agreement | green — `KERNEL_SU_UAPI_VERSION = 6` at both bases, equal to `uapi_anchor`; `KSU_FEATURE_MAX` unchanged |
| G3 ksud Android compile | covered by the same batch's prune commit — this step changed no Rust source |
| G4 Manager compile | covered by the same batch — this step changed no Kotlin, gradle or asset |

## Needs human

Nothing new from this range. The pre-existing items stand: the AdSense slot and `SITE_URL` still name upstream, and the docs site has never been deployed. Both are handled separately in the batch that follows this advance.
