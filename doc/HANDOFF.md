# Handoff

Written for whoever picks this up next. Read it for repository state, what to do next, and which traps are already known.

## What this is

BreezeKSU is a **trimmed fork** of [KernelSU](https://github.com/tiann/KernelSU). The goal is subtraction — delete what the fork does not need, keep a clean kernel-level root solution — plus a small set of fork-owned features.

Current state: **infrastructure ready, pruning not started.**

## Repository state

| Item | Value |
| :--- | :--- |
| Baseline | `6d31a6137`, recorded as `upstream_base` in `doc/PRUNING.md` — a value, not a branch |
| Branches | `dev` = where work lands; `main` = landing line — verified batches are **squashed onto it**, so it reads as finished changes. `dev` is currently 2 commits ahead (infrastructure, then content) |
| Remotes | `origin` → github.com/u9521/BreezeKSU; `upstream` → tiann/KernelSU |
| Git | `rerere.enabled=true`, `merge.conflictStyle=zdiff3` |
| Branding | `u9521.breezeksu` / `BreezeKSU`, signing size `0x01ed`, hash `3ebb1bb9…` |
| UI | inherits `breezeui/` (151 files) alongside upstream's Material/Miuix |
| Device | AVD `emulator-5554` (x86_64, Android 17, guest kernel v3.3.0-70 / UAPI 5). See §6 |

## The three documents that matter

| Document | Describes | Maintained by |
| :--- | :--- | :--- |
| [`FUNCTION_MAP.md`](FUNCTION_MAP.md) | **what upstream has**: four-layer function map with file:line, coupling, removability | a sync may rewrite it wholesale; it describes upstream |
| [`PRUNING.md`](PRUNING.md) | **what this fork removed**: anchor block, the residue surface list, and an index into [`entries/`](entries/) | **humans and pruning sessions only**; sync verifies, never edits |
| [`entries/`](entries/TEMPLATE.md) | one file per pruning or fork-owned feature: why, surface, residue verdicts, verification, sync risk, rollback | written with the change; never deleted |
| [`seams.md`](seams.md) | the contact surface with upstream: modified files and added trees, with what each depends on | updated whenever an upstream file is modified |
| [`decisions/`](decisions/README.md) | settled decisions in one file, each with the question it answers | append a section; superseded reasoning goes to git |

The split is deliberate: `FUNCTION_MAP.md` is the fact layer and can be refreshed by upstream evolution; `PRUNING.md` is the decision layer and only changes through a human decision. Merging them would let an upstream update erase the fork's own annotations.

## What to do next

### Pruning (the main work)

`PRUNING.md` is empty — nothing has been deleted yet. Walk `FUNCTION_MAP.md` by ID and invoke the `breeze-prune` skill per feature.

Suggested order (value for effort, from the FUNCTION_MAP analysis):

| Order | Target | Main saving |
| :--- | :--- | :--- |
| 1 | Module repository (M5) | ~4.1k lines + 21 strings |
| 2 | One-shot scripts (M7) | ~1.8k lines + 36 strings |
| 3 | Theming collapse (M9, including the double UI) | ~3k lines |
| 4 | Sulog (M8 + K5.3 + U6.2) | 1.2k Manager + 801 ksud + 431 kernel |
| 5 | Template system (part of M3) | 1.1k lines |
| 6 | boot-patch-v2 (U4.2) | 4183 lines of Rust |
| 7 | selinux_hide (K5.5) | 1257 kernel lines + 475 lsm_hook |

Each removal updates `PRUNING.md` in the same change: ID, mechanism, residue, verification, reason.

### Three lines that cannot be deleted

- `init.rc` injection (`kernel/runtime/ksud_integration.c`) is the module system's only start point; deleting it removes post-fs-data/services/boot-completed entirely.
- The UAPI base (`uapi/`, `supercall/`, the `feature` framework) is the channel every switch travels through.
- boot-patch's **probing** half cannot go: the Manager install path, five `boot-info` subcommands and `uninstall`'s boot restore all depend on it. Lift probing out first, then delete patch/restore.

### Syncing (low frequency, human-triggered)

Invoke `breeze-upstream-sync`. It ports upstream behaviour as fork commits — there is no `git merge` step — and calls `breeze-sync-gates` when done. A big sync (advancing the baseline) only happens when a human asks for it.

## Five traps (all hit already)

1. **Module parameters are an invisible hard dependency.** Remove `allow_shell`/`norc`/`bundled`/`block_modules`/`preinit`/`force_feature` from the kernel while bootconfig still carries the key and modules **fail to load**. Stop `ksud` writing those keys first.
2. **Feature ids and ioctl numbers are append-only.** `KSU_FEATURE_MAX` is still 7; id 5 is deliberately unused upstream. Prune by not registering the handler — **never recycle a number**, or an old `.feature_config` shifts onto a different feature.
3. **`git show <branch>:<path>` whole-file overwrite is wrong.** The branch's base may predate the current baseline, so overwriting silently reverts updates made after it. Add new files wholesale; for modified files reset to `main` first, then three-way apply with `git apply -3`.
4. **UAPI is hard-coupled.** `ksud`'s `ensure_uapi_version_matched()` bails on any difference; the Manager's `isFullFeatured()` degrades the whole UI on any difference. Kernel, ksud and Manager ship together.
5. **A pruned feature's Breeze page must go too.** `breezeui/screen/<feature>/` carries a `*Breeze.kt` plus `*Entry.kt` pair; leaving them behind leaves references to a deleted ViewModel.

## Environment

```bash
# ksud (use cross when cargo-ndk is missing; the justfile does)
cd userspace/ksud && cargo ndk -t arm64-v8a check
#   or: cross build --target aarch64-linux-android

# Manager (binaries first, one per ABI)
mkdir -p manager/app/src/main/jniLibs/arm64-v8a manager/app/src/main/jniLibs/x86_64
cp userspace/ksud/target/aarch64-linux-android/release/ksud \
   manager/app/src/main/jniLibs/arm64-v8a/libksud.so
cp userspace/ksud/target/x86_64-linux-android/release/ksud \
   manager/app/src/main/jniLibs/x86_64/libksud.so
cd manager && ./gradlew :app:assembleDebug
```

Kernel builds need the DDK and a target kernel tree, normally unavailable locally; kernel correctness rests on the rule "kernel changes only travel through the kernel-compat lane".

### 6.1 Running it on the AVD (currently the only verification path)

WSL has no JDK 21 (only 25) and the Gradle wrapper wants 9.7.x while the local cache has 9.5.0, so **do not use WSL's `./gradlew` for the Manager**. Go through Android Studio:

1. Android Studio has the project open at the **Windows** path `C:/opt/project/AndroidStudioProjects/BreezeKSU`. All `android_studio` MCP tools must be given that string; WSL paths are rejected ("doesn't correspond to any open project").
2. `build_project` returns the error list under `structuredContent.problems[].kind == "ERROR"`, each with `file`/`line`. It runs a full `:app` compile — the G4 gate in this environment.
3. `execute_run_configuration` with the **`app`** configuration installs and launches. It blocks for a long time and often reports `Request timed out` after actually succeeding — check `dumpsys package` for `lastUpdateTime` before re-running.
4. Screenshots need an explicit display id, or `screencap` returns a one-line warning instead of a PNG:

```bash
DISP=$(adb -s emulator-5554 shell "dumpsys SurfaceFlinger --display-id" | head -1 | awk '{print $2}')
adb -s emulator-5554 exec-out screencap -p -d "$DISP" > /tmp/shot.png
od -An -t x1 -N 4 /tmp/shot.png     # must be 89 50 4e 47
```

5. **The AVD kernel was replaced with a UAPI 6 build on 2026-10-21.** `ksud debug info` reports `version: 32703`, `uapi_version: 6`, `flags: 0x31`, `features: 0x7`, `bundled: true`. With that, `isFullFeatured()` holds and **all four Breeze bottom-navigation tabs appear**. Criterion: `uapi_version` equals `KERNEL_SU_UAPI_VERSION` (6) and `flags` contains `0x2` (MANAGER). An older UAPI 5 kernel makes `isFullFeatured()` false and the bottom bar disappears — that is upstream's designed degradation, not a bug.
6. **The on-device ksud is still old** (`ksud 3.3.0-70`, uapi 5). The kernel supports feature id 6 (`shell_su_adb_only`, shown by `features: 0x7`) but the old ksud does not: `ksud feature check shell_su_adb_only` → `Error: Unknown feature`. The Manager reads that state through `ksud feature check` (`KsuCli.kt:117`), so the "Restrict shell root to ADB" row is correctly greyed out (`enabled = status == "supported"`). This is live evidence that UAPI is hard-coupled — not a fork bug. To exercise the row, install the ksud built in this round into `/data/adb/ksud`.
7. When checking tab switching by screenshot, take `input tap` y-coordinates from the nav label's actual bounds (about y≈2225 here), not from a screen ratio — ratio-based taps land in the content area and produce a screenshot identical to the home page.

## Commit convention

Commits follow upstream's `<scope>: <summary>`: a short lowercase scope for the touched area, a concise sentence-case summary, no trailing period.

```
manager: drop module registry
ksud: drop boot-patch-v2
docs: prune M5, M7
```

Fork commits are told apart from upstream's by their author and by the fact that they sit on `dev`, not by a prefix.

A commit that **ports upstream code** also carries `Co-authored-by` trailers naming that code's upstream authors, taken verbatim from `git show -s --format='%an <%ae>' <sha>`. The trailer exists to credit the person who wrote the code, so nothing else — no tool, assistant or session — ever appears in it. Fork-owned work that ports nothing (docs, branding, `breezeui/` skin) carries no trailer.
