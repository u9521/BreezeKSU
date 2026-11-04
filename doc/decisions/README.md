# Decisions

Settled decisions for BreezeKSU. Each keeps the question it answers — without the question, a later reader cannot tell what the decision covers — and states the answer as it stands now.

Rejected alternatives and superseded reasoning live in git history. This file describes what is true.

---

## 1. Source of truth

### Question

Where is the authoritative record of how BreezeKSU differs from upstream?

### Answer

**Commit history is the source of fact; the pruning result exists as a document an agent reads and then merges semantically.**

The pruning **result** is written as a document, not a replayable machine recipe — an agent reads it, nothing executes it. A separate map of upstream ("what upstream has") sits alongside it, saying which parts were removed, why, and how far.

| Concept | Carrier | Written by | Read by |
| :--- | :--- | :--- | :--- |
| Upstream state (what exists, where, what it couples to) | [`FUNCTION_MAP.md`](../FUNCTION_MAP.md) | a human or agent during sync | sync agents, humans |
| This fork's choices (removed, kept, modified) | [`PRUNING.md`](../PRUNING.md) | written while pruning | sync agents, humans |
| The actual code delta | git history | committed while pruning | git itself |

Consequences:

- The sync algorithm is **semantic**: an agent reads the upstream diff plus both documents and judges "does this land in a pruned feature, does it need porting" rather than applying patches mechanically.
- Document **freshness is a precondition for correctness** — a stale document makes the agent misjudge. The sync skill's completion criteria include "update the documents"; it is not a bonus step.
- The documents need machine-checkable anchors: `uapi_anchor`, `upstream_base`, the removed path and symbol lists, and the protected constants.

---

## 2. Sync algorithm

### Question

Between the recorded upstream baseline and a new upstream, by what algorithm does a skill move upstream behaviour into an already-pruned tree?

### Answer

Classed routing, not one strategy:

```
1. Scope       upstream base → new upstream; list every commit and diff
2. Classify    seven classes (see §5), marked must-port / decision / skip
3. Route       does the diff hit PRUNING.removed_paths ?
                 ├─ hit  → general fix extraction only
                 └─ miss → port
4. Port        one commit per feature; never a git merge
5. Ledger      record upstream SHA set → fork SHA → feature name
6. Seams       re-verify every modified upstream file and added fork tree
7. Docs        FUNCTION_MAP.md follows upstream; PRUNING.md verified only
8. Gates       the four checks (see §3)
```

### General fix extraction

A commit inside a pruned area is not skipped wholesale; judge whether it carries a portable part:

| Case | Action |
| :--- | :--- |
| the fix only concerns a deleted file's internals | skip, record in the skipped list |
| the fix is cross-file and the deleted file is only affected | hand-port the non-deleted parts |
| the fix changes UAPI or a shared structure | **stop**, human decision (it moves the UAPI anchor) |

### Conflict precedence

1. `protected_constants` hit → keep the fork value, report it.
2. Seam files (S1/S2/S4, `SuFilePathHandler`) → keep the seam, accept the rest of upstream.
3. Inside `ui/` → normally zero conflict (upstream does not touch it).
4. Everything else → judge by the classes in 5.

### Why ports and not merges

A `git merge upstream/main` drags upstream's new features in wholesale and then forces a pruning pass to remove them again — the cycle this fork exists to avoid. Porting feature by feature keeps "upstream behaviour arrives" and "the fork's scope is preserved" as two separately reviewable acts. Rebase, used by a big sync, moves the base without merging either.

---

## 3. Verification gates

### Question

After a sync, what proves "nothing broke", and what proves "pruning was not quietly undone by upstream"?

### Answer

Four gates, ordered by cost, stop at the first failure. They are executed by the `breeze-sync-gates` skill rather than a script: G1 and G2 need judgement — is this symbol a live reference or a dead one? — and a script that guesses produces false green.

| # | Gate | Criterion | On failure |
| :--- | :--- | :--- | :--- |
| G1 | **Pruning integrity** | every path in `PRUNING.removed_paths` is still absent; every symbol in `removed_symbols` still has no reference | stop, list path/symbol with line numbers |
| G2 | **UAPI agreement** | kernel `uapi/supercall.h`, the ksud binding, and the Manager's `Natives.MINIMAL_SUPPORTED_KERNEL` all match `PRUNING.uapi_anchor` | stop, human decides |
| G3 | **ksud compiles** | `cargo ndk -t arm64-v8a check` | report, locate the file |
| G4 | **Manager compiles** | stage `jniLibs/libksud.so`, then `./gradlew :app:assembleDebug` | report, attach the first error |

### Why G1 is a gate and not a report

The most common way upstream brings a feature back is not a conflict. It is a new file referencing a removed symbol, or a refactor moving logic into a file that still exists. Both may compile — the reference sits in a dead branch — and only a path and symbol sweep catches them.

### Kernel compilation is not gated

`make -C kernel` needs a target kernel tree and the DDK: expensive and environment-heavy. G1–G4 are the sync gate; kernel correctness rests on the rule that kernel changes only travel through the kernel-compat lane (see §4).

---

## 4. Decay while frozen

### Question

A long-frozen Android kernel solution decays in several ways. Which of them must follow upstream, and on what rhythm?

### Answer

**UAPI is the fastest-decaying contract, and it fails hard.** Upstream moved `KERNEL_SU_UAPI_VERSION` six times in roughly five months, with a peak of one breaking change every 3–6 weeks. All three layers are hard-coupled: `ensure_uapi_version_matched()` in `ksucalls.rs` bails on any mismatch, `isFullFeatured()` degrades the whole Manager UI on any mismatch, and legacy ioctls backfill `uapi_version = 0` so they never match.

**Kernel internals decay silently.** LSM hook layout, `static_call`, the seccomp filter struct copy, and `kallsyms` resolution can all shift without a compile error — the hook simply stops firing. These need a device run, not a build.

**Deleting a decay source beats following it.** `selinux_hide` consumes SELinux private structures directly and is the fastest-decaying kernel block; removing it deletes its row entirely and takes `lsm_hook` with it.

### Lanes

| Lane | Content | Cadence |
| :--- | :--- | :--- |
| kernel-compat | only patches carrying `KERNEL_VERSION` | every 3 months, or within 1 month of a new KMI |
| uapi | locked at `UAPI=6`; moves only on a deliberate bump decision, all three layers in one commit | event-driven |
| ui | drift between `ui/` and the upstream contract | alongside upstream UI changes |

Dependencies are **not** a lane: the frozen object is the commit, not the versions (see §10).

### Safe to freeze indefinitely

The module format (`module.prop`, lifecycle scripts), supercall ioctl numbers and struct layouts once UAPI is locked, the WebUI JS API, documentation and translations.

---

## 5. Upstream change classes

### Question

Between an old baseline and a new upstream there are hundreds of commits. What rule decides port / skip / modify?

### Answer

Seven classes; the first three must be ported.

| Class | Signal | Default action |
| :--- | :--- | :--- |
| **Security fix** | CVE / bounds check / out-of-bounds / UAF / privilege | **must port** |
| **Crash or corruption** | "fix crash" / "avoid panic" / null deref / race | **must port** |
| **Kernel compatibility** | diff contains `KERNEL_VERSION` / new KMI | **must port** (kernel-compat lane) |
| **UAPI / ABI** | `uapi/` change / `KSU_FEATURE_MAX` / ioctl number | **separate decision**, all three layers ship together |
| **New feature** | `feat:` / new directory / new ioctl | skip, record in the skipped list |
| **Refactor** | pure move or rename, no behaviour change | skip; re-evaluate if it moves code out of a pruned area |
| **Docs / CI / translations** | `website/`, `.github/`, `values-*` | skip |

### Commits inside pruned areas

Not skipped automatically. Apply the general fix extraction rules in 2 — the question is whether the deleted file is the **subject** of the fix or merely **one of the affected** files.

### Skipped list

Every sync produces `doc/sync/<date>/skipped.md`: commit sha, title, class, reason, whether it needs re-evaluation next time. This is the only record that a skip was deliberate.

### Who decides

A skill classifies automatically. **UAPI-class changes and any fix landing in a pruned area always go to a human**; everything else executes automatically.

---

## 6. Re-freeze

### Question

When the fork can no longer keep up, what counts as "cannot keep up", and what does re-freezing do?

### Answer

Triggers — any one prompts consideration:

| Signal | Threshold |
| :--- | :--- |
| **UAPI behind** | upstream `KERNEL_SU_UAPI_VERSION` exceeds `uapi_anchor` by ≥2 |
| **Conflict surface** | more than 20 conflicting files in one sync, or ≥3 seam files needing edits |
| **KMI generation break** | upstream supports a new KMI the fork's `kernel/` is incompatible with |
| **Manager toolchain break** | AGP / Kotlin need two major versions to reach a new targetSdk |

What a re-freeze does:

```
1. Take a fresh upstream snapshot as the new base
2. Restart dev on it:
   a. replay the Breeze UI skin (can be taken wholesale)
   b. replay the branding constants
   c. replay the pruning, entry by entry from PRUNING.md
3. Archive the old tree: tag `freeze/<old base sha>`, keep the branch but stop maintaining it
4. Update PRUNING.upstream_base and uapi_anchor
```

The baseline is a recorded value, not a branch. `main` is the landing line — verified batches are squashed onto it — so a re-freeze restarts `dev` on the new snapshot and leaves `main` to receive the finished result.

### Why replay instead of continuing to merge

If a trigger fires, incremental merging already costs more than a replay. The replay's cost is "prune again", but that part is mechanical: every `PRUNING.md` entry records its mechanism and residue, so the replay is a checklist.

### The old tree can still take security patches

Only **security** patches that do not touch pruned areas, on a separate `freeze/<sha>-hotfix` branch. Anything beyond that belongs in a new freeze.

### Distinct from a baseline advance

A baseline advance moves `upstream_base` and replays the commits onto the new base — minutes, no pruning redone. Re-freeze is the expensive one. `PRUNING.md` keeps the two definitions apart.

---

## 7. Branding and signing

### Question

BreezeKSU must build under its own package name and signature, otherwise the kernel's `apk_sign.c` and `throne_tracker` do not recognise the Manager and the root-authorisation chain breaks.

### Answer

| Item | Value |
| :--- | :--- |
| Package (release) | `u9521.breezeksu` |
| Package (PR) | `u9521.breezeksu.pr` |
| App name | `BreezeKSU` / `BreezeKSU PR` |
| Signature size | `0x01ed` |
| Signature hash | `3ebb1bb93ce21987d5c467211d0617b9ce03565fb17acaa865d56dfd4cd395ee` |

Two landing sites, both upstream-provided override points:

- `kernel/Kbuild` — `KSU_IS_PR_BUILD` / `KSU_PACKAGE_NAME` / `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH`
- `manager/app/build.gradle.kts` — `IS_PR_BUILD` / `KSU_PACKAGE_NAME` / `KSU_NAME`

Both sides distinguish PR builds, so a PR Manager ships the release name only when it also ships the release signature; mismatched pairs are judged "not the Manager".

### Sync protection

Both sites live in files upstream changes. They are the `protected_constants` in the `PRUNING.md` anchor block: when an upstream diff touches `KSU_PACKAGE_NAME` / `KSU_EXPECTED_*` / `defaultManagerPackageName` / `defaultManagerName`, keep the fork value and report it; never accept upstream's silently.

---

## 8. Repository layout

### Question

What do the branches, remotes and the baseline mean now that more than one of them could hold "the upstream state"?

### Answer

| Item | Value |
| :--- | :--- |
| Remotes | `origin` → `github.com/u9521/BreezeKSU`; `upstream` → `tiann/KernelSU` |
| `dev` | where work lands |
| `main` | the landing line: verified batches are **squashed onto it**, so it reads as finished changes |
| Baseline | `upstream_base` in `PRUNING.md` — a value, never a branch tip |
| Git config | `rerere.enabled=true`, `merge.conflictStyle=zdiff3` |

`main` carries fork work, so "what did the fork change" is derived against `upstream_base`, not against `main`:

```bash
base=$(sed -n 's/^upstream_base: *\([0-9a-f]\{7,\}\).*/\1/p' doc/PRUNING.md)
git diff --name-status "$base" dev | grep -v '^A'
```

### Overwriting an upstream file wholesale is wrong

`git show <branch>:<path>` restores a file as it was at that branch's base, which may predate the current baseline — so it silently reverts everything upstream did to that file since. Add new files wholesale; for modified files, reset to the baseline first and then three-way apply with `git apply -3`.

---

## 9. Skill shape

### Question

What do the sync skills look like, how are they invoked, and where do they end?

### Answer

**User-invoked** (`disable-model-invocation: true`), zero always-loaded context. Syncing is low-frequency, decision-heavy work that does not need an agent to discover it on its own.

`breeze-upstream-sync` holds the steps, each with a completion criterion. Its `reference/` carries the triage classes, the conflict policy, and the fork asset inventory. The gates live in a separate `breeze-sync-gates` skill so they can run on their own — after a hand-resolved conflict, for example — while the sync calls them as its last step: one body of text, two entry points.

Each step writes its output to `doc/sync/<date>/`, so an interrupted sync resumes from the last completed step instead of redoing work already on disk.

Hand-back to a human is not failure: UAPI-class commits, a fix inside a pruned area whose subject is unclear, and any conflict the policy cannot resolve are all listed rather than decided by the agent.

---

## 10. Dependencies

### Question

Should dependency versions be pinned or audited while the fork is frozen?

### Answer

**No deps lane, no pinning, no audit gate.**

The frozen object is the **commit**, not the dependency versions. `Cargo.lock` and `libs.versions.toml` stay as they were at the baseline; there is no ban on `cargo update` either.

When a dependency causes a concrete production problem — a device cannot install, an API stops working, a security advisory fires — upgrade in place. That is an ordinary local change: it does not go through the sync mechanism and does not appear in a sync report.

No `cargo audit` / `cargo deny` gate: the zip-extraction attack surface is a real fact, but it is knowledge to apply when relevant, not a process checkpoint.

The documents therefore **do not record dependency versions** and do not record a "last dependency sync point" — recording them puts back the state the decision removed.

---

## 11. Document shape

### Question

How is the pruning result written so an agent can read it, while keeping a map that describes upstream?

### Answer

Two independent documents, each updated on its own terms, neither overwriting the other.

| Document | Describes | Updated by | During a sync |
| :--- | :--- | :--- | :--- |
| [`FUNCTION_MAP.md`](../FUNCTION_MAP.md) | **what upstream has**: four layers, subsystems, file:line, coupling | the agent, from the upstream diff | may be rewritten by upstream evolution; carries no fork decisions |
| [`PRUNING.md`](../PRUNING.md) | **what this fork removed, why, and how far** | written while pruning, verified during sync | fork-maintained only; never accepts an upstream value |

The function map is the **fact** layer and can be refreshed by upstream evolution; the pruning table is the **decision** layer, and changing it requires a human. Merging them would let an upstream update erase the fork's annotations.

### Fields per pruning entry

```
ID          — the FUNCTION_MAP K/U/M/P identifier
Status      — removed / kept / modified
Mechanism   — block delete / surgery / runtime off
Residue     — what still needs checking (ioctl numbers, feature ids, string keys, Manifest components, Breeze pages)
Verified by — how the removal was proven (compile / grep / run)
Why         — one line for the next reader
```

### The anchor block

A fixed section at the top of `PRUNING.md`, read by the gates:

```yaml
uapi_anchor: 6
upstream_base: <sha>
removed_paths:        # wholly deleted dirs/files; an upstream change here is an error
removed_symbols:      # deleted symbols/ioctls/feature ids; numbers stay reserved
protected_constants:  # keep the fork value, never upstream's
```

### Update responsibility

- The sync skill's completion criteria include "both documents updated". A stale document means correctness is gone (see §1).
- The pruning table is written only by humans or pruning sessions; a sync session reads and verifies, never edits a decision.

---

## 12. The Breeze skin

### Question

How does the fork's own UI relate to upstream's Material and Miuix implementations?

### Answer

**The skin is inherited and owned, and it is now the only renderer.** `manager/.../ui/` is part of BreezeKSU's product shape, not a prunable upstream feature; the pruning table never lists it as a removal.

- **A pruned feature takes its Breeze page with it.** Removing `sulog` means removing the whole `ui/screen/sulog/` triple — `SulogViewModel.kt`/`SulogUiState.kt`, `SulogEntry.kt`, `SulogBreeze.kt`. Every pruning entry lists its Breeze-side residue.
- There is **no skin switch** to keep or prune. `UiMode` and the `breeze`/`material`/`miuix` settings row were deleted (F1, F4); a pruning that touches the UI layer cannot leave a mode variable behind, because none exists.

### How it attaches

It does not attach itself — it *is* the app. `MainActivity` calls `BreezeApp` unconditionally and `WebUIActivity` calls `BreezeWebUIRoot` unconditionally; there is no `when (uiMode)`, no early return, no second renderer, and no enum for upstream to reshape. Both entry points go through the one `BreezeShell`, which supplies the theme, the blur flag and the shared overlay window.

The settled shape is five layers with a single direction, enforced by a gate rather than by review:

```
data|core|magica|profile + Natives|Kernels|KernelSUApplication
  → ui/{theme,icons,util}
    → ui/component/{common,breeze}
      → ui/{nav,animation}
        → ui/screen/<feature>          ( *ViewModel/*UiState → *Entry → *Breeze )
        → ui/webui                     (sibling host, never reaches ui/screen)
composition root: ui/{BreezeApp,BreezeShell,MainActivity}
```

The only surviving relic of the old seam is the `ui_mode` string `SuFilePathHandler` still reads, defaulting to `"breeze"`: an installed app written by an earlier build carries it forward, so the read stays even though nothing writes a second value.

### Change priority is a preference, not a rule

1. New files first (the whole `ui/` package, including each page's triple).
2. Minimal delta second (a line in `SuFilePathHandler`).
3. Modifying upstream files last.

The fork may modify an upstream file freely when that is the clearer change; the port rule is the hard one — never copy upstream's structure, re-express the behaviour in Breeze. What is mandatory is recording each modified upstream file in the seam inventory ([seams.md](../seams.md)), because otherwise the next sync cannot notice the edit disappearing — "modify freely" would silently become "lose the change".

---

## Standing structure

- **Two documents, two owners.** `FUNCTION_MAP.md` describes upstream and may be rewritten by it; `PRUNING.md` records fork decisions and changes only by human decision.
- **Three lanes, no fourth.** kernel-compat (quarterly), uapi (locked, event-driven), ui (alongside upstream UI changes). Dependencies are not a lane.
- **Four gates.** G1 pruning integrity, G2 UAPI agreement, G3 ksud compile, G4 Manager compile — run by `breeze-sync-gates`.
- **Ports only.** Upstream never merges in; ports carry `Co-authored-by` for whoever wrote the code upstream.
- **The baseline is a value.** Recorded in `PRUNING.md`, never read off a branch.

## Not yet decided

- **First pruning batch and order.** The gates and the entry fields are ready; the order is left to whoever executes, using `FUNCTION_MAP.md`.
- **Architecture scope.** Whether arm64 / x86_64 / riscv64 are all kept.
- **Five open forks** in `FUNCTION_MAP.md` §7: download-and-patch, metamodule, per-app policy, double UI, LKM/late-load.

## Out of scope

- Redesigning upstream itself; rejected upstream designs are not sent back upstream.
- Performance work and new features unrelated to pruning; this effort subtracts and ports.
