---
name: breeze-sync-gates
description: Run the four BreezeKSU gates after syncing upstream or pruning a feature — pruning integrity, UAPI agreement, ksud compile, Manager compile. Use when asked to verify a sync or prune, "run the gates", to check nothing deleted came back, or to confirm the three layers still agree.
disable-model-invocation: true
---

# Sync gates

Four gates, run in order, stop at the first red one. The judgement stays with you: these are checks to perform and report, not a script that returns green.

`breeze-upstream-sync` step 8 and `breeze-prune` both call this skill; they do not restate it.

## G1 — Pruning integrity

Read the anchor block in `doc/PRUNING.md` and check each entry by hand.

**Paths.** Every path in `removed_paths` must still be absent.

```bash
# for each path P in removed_paths:
test -e "$P" && echo "RESTORED: $P"
```

**Symbols.** Every symbol in `removed_symbols` must have no live references. Search the layers that could reintroduce it:

```bash
rg -n '<symbol>' --glob '*.c' --glob '*.h' --glob '*.rs' --glob '*.kt' --glob '*.java'
```

**Seams.** Every row of `doc/seams.md` that exists to reach fork code must still reach it, and its `Depends on` symbols must still exist. A seam whose dependency vanished is the same failure as a restored path: the fork's edit is gone, but the file still compiles because the import went out with the feature.

```bash
# for each symbol S in the inventory's Depends on column:
rg -n "$S" <path from the inventory row>
```

**Why a human runs this**: the common way a removed feature comes back is not a merge conflict. It is a new upstream file that calls a removed symbol, or a refactor that moves logic into a file which still exists. Both compile — the reference lands in a branch that never runs — so only a path and symbol sweep catches them. The seam sweep catches the mirror-image failure: a seam that survived while the thing it reached was pruned away.

A hit is a failure: report path, symbol, file and line, then stop.

## G2 — UAPI agreement

Three declarations must equal `uapi_anchor` from `doc/PRUNING.md`:

| Layer | Where |
| :--- | :--- |
| Kernel | `uapi/supercall.h` — `KERNEL_SU_UAPI_VERSION` |
| Daemon | `userspace/ksud/src/ksucalls.rs` — the binding's version constant |
| App | `manager/.../Natives.kt` — `MINIMAL_SUPPORTED_KERNEL` and `managerUAPIVersion` |

Also confirm `KSU_FEATURE_MAX` and the reserved feature id are unchanged: numbers are append-only.

Any mismatch stops the run and goes to a human — a UAPI bump ships all three layers at once, and the anchor change is a decision, not a gate fix.

## G3 — ksud compiles for Android

```bash
cd userspace/ksud && cargo ndk -t arm64-v8a check
# without cargo-ndk:
cross build --target aarch64-linux-android
```

Report the first error with file and line. Do not auto-fix: a compile error here usually means the port is incomplete, and guessing hides that.

## G4 — Manager compiles

Stage the binaries first, then build:

```bash
mkdir -p manager/app/src/main/jniLibs/arm64-v8a
cp userspace/ksud/target/aarch64-linux-android/release/ksud \
   manager/app/src/main/jniLibs/arm64-v8a/libksud.so
cd manager && ./gradlew :app:assembleDebug
```

Where the wrapper's JDK or Gradle version is unavailable (commonly from WSL), build through Android Studio with the repository opened at its Windows path and read its error list. Report the first error; do not auto-fix.

## Not gated: the kernel

Kernel compilation needs a target kernel tree and the DDK, so it cannot be a local gate. Correctness there rests on the kernel-compat lane: kernel changes arrive as their own commits and get one device run before landing.

## Reporting

Post the command and its real output for each gate, in order. State explicitly which gates ran, which were skipped, and why a skip is acceptable. "All green" without output is not a result.
