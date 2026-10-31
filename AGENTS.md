# AGENTS.md

Entry projection for agents: repository state and pointers, never rule bodies. `AGENTS.md` and `.agents/` are detachable — deleting both leaves a repository that still installs, builds and ships.

## What this repo is

BreezeKSU is a trimmed fork of KernelSU, a kernel-based root solution for Android. It ships a kernel module, Rust userspace daemons, a Kotlin Manager app, and docs/web assets.

The fork **removes** upstream capabilities it does not want and **adds** its own. Upstream supplies behaviour, not scope: recoveries and fixes are ported in deliberately, one feature at a time, as fork commits. Work lands on `dev`; once a batch is verified it is **squashed onto `main`**, so `main` reads as a sequence of finished changes rather than a working history. The upstream baseline is a value recorded in `doc/PRUNING.md`, not a branch.

## Directory map

| Path | What lives there | Read before changing it |
| :--- | :--- | :--- |
| `kernel/` | C kernel module: supercall, hooks, policy, features, runtime injection | [kernel](.agents/architecture/kernel.md) |
| `userspace/ksud/` | Rust daemon and CLI; owns modules, metamodule, boot patching | [userspace](.agents/architecture/userspace.md) |
| `userspace/ksuinit/` | LKM bootstrap init and ELF loader | [userspace](.agents/architecture/userspace.md) |
| `manager/` | Kotlin/Compose Manager app, including the `ui/` skin | [manager](.agents/architecture/manager/index.md) — then [build](.agents/architecture/manager/build.md), [ui](.agents/architecture/manager/ui.md), [overlay](.agents/architecture/manager/overlay.md), [overlay stack](.agents/architecture/manager/overlay-stack.md), [markdown](.agents/architecture/manager/markdown.md) |
| `uapi/` | Kernel↔userspace ABI contract, versioned | [contracts](.agents/architecture/contracts.md) |
| `website/`, `js/` | VitePress docs site and the module WebUI library | — |
| `doc/` | Fork ledger: handoff state, pruning table, upstream function map, glossary, decision records, sync ledger | start at `doc/HANDOFF.md` |
| `.agents/skills/` | Behaviour specs invoked by trigger | each `SKILL.md` description |
| `.agents/architecture/` | Per-area constraints and invariants | — |
| `.agents/gates/` | Documentation and scaffold gates | — |

## Command matrix

| Purpose | Command |
| :--- | :--- |
| Spec gates (docs, scaffold, links) | `node .agents/gates/verify-all.mjs` |
| Verify ksud on the Android target | `cd userspace/ksud && cargo ndk -t arm64-v8a check` |
| Same, without cargo-ndk | `cross build --target aarch64-linux-android` |
| Stage ksud binaries into the Manager | see [userspace](.agents/architecture/userspace.md) |
| Build the Manager | `cd manager && ./gradlew :app:assembleDebug` |
| Build the docs site | `cd website && bun install && bun run docs:build` |

There is no test suite. Substitutes: the spec gates above, the Android-target compile for Rust, a full `:app` compile for Kotlin, and on-device smoke runs.

## Where to look next

- What the fork removed and why → `doc/PRUNING.md`.
- What upstream contains and what is removable → `doc/FUNCTION_MAP.md`.
- Repo state, known traps, device environment → `doc/HANDOFF.md`.
- Settled decisions and rejected alternatives → `doc/decisions/`.
- Why a term is spelled the way it is → `doc/GLOSSARY.md`.
- Syncing upstream, pruning a feature, running gates → `.agents/skills/`.

## Three rules

- Read the architecture page for a path before changing it; the invariants there are not negotiable.
- Port upstream changes by feature, as ordinary fork commits. A bare `git merge` is off the table.
- Keep `doc/PRUNING.md` true in the same change that alters what the fork removed or added.
