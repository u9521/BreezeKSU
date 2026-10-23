---
name: breeze-upstream-sync
description: Port upstream tiann/KernelSU behaviour into the trimmed BreezeKSU fork as fork commits. Use when the user asks to sync upstream, follow a new release, pick up a kernel-compat patch, run a big sync, or asks "what changed upstream".
disable-model-invocation: true
---

# Upstream sync

BreezeKSU removed part of KernelSU and added its own features. Sync ports **behaviour** in; it never lets a removed feature return by accident.

Two modes, and the difference matters:

- **Feature sync** (default): port a specific upstream feature or fix.
- **Big sync**: advance the whole baseline. Human-triggered only — never start one because the calendar says so.

Neither mode runs `git merge`. Every change lands as a fork commit.

## Before starting

Read, in this order:

- `doc/PRUNING.md` — the anchor block (`uapi_anchor`, `upstream_base`, `removed_paths`, `removed_symbols`, `protected_constants`) is the basis for every decision below.
- `doc/sync/ledger.md` — what was already ported, with upstream and fork SHAs.
- `doc/seams.md` — the contact surface: modified upstream files and added fork trees, with what each depends on.

Completion: all three loaded, and the anchor values are extracted as concrete values you can cite.

## 1. Scope

Ask which upstream range applies. For feature sync the user names the feature; for big sync take the range from `upstream_base`.

```bash
git fetch upstream --prune --tags
git log --oneline <upstream_base>..upstream/main
```

Completion: the commit list is written to `doc/sync/<date>/commits.txt`.

## 2. Triage

Classify every commit with `reference/triage-rules.md`. A commit already listed in the ledger is marked `ported` and removed from the work queue.

Completion: every commit carries a class; no `unknown` remains. Written to `doc/sync/<date>/triage.md`.

## 3. Route

For each commit that will be ported, check whether its diff touches `removed_paths`:

- **Hit** → apply `reference/conflict-policy.md` "general fix extraction" and decide whether the deleted file is the subject of the fix or merely affected by it.
- **No hit** → port it.

Completion: every ported commit has a route conclusion (port / extract / skip / needs-human).

## 4. Port

Port by **feature**, not by commit. A feature is one upstream change set, often several commits; it becomes one fork commit (or a small ordered group) that reads as a single complete action.

```bash
git cherry-pick -n <sha>          # or apply the diff by hand after a reset
# resolve, build, then commit
```

Rules for the commit:

- Subject in upstream's `<scope>: <summary>` form, no prefix: `<scope>: port <feature>`.
- Trailers name **the upstream author of the code being ported** — the person who wrote that feature upstream, taken verbatim:

  ```bash
  git show -s --format='%an <%ae>' <sha>
  ```

  One line per distinct upstream author; when the original commit itself carries `Co-authored-by` entries naming humans, carry those too. Never put an assistant, tool or session in this trailer.
- Trailers go in the body after a blank line.
- The trailer does not apply to fork-owned commits that port nothing — documentation, branding, `breezeui/` skin work.

Big sync advances the baseline instead of porting feature by feature: rebase the fork commits onto the new upstream tip, drop anything the ledger marks as already ported, and verify with `git range-diff`. Only a human triggers this.

The baseline lives in `doc/PRUNING.md`, not in a branch — `main` carries fork documentation and is not a clean upstream mirror. Work from the recorded SHA:

```bash
base=$(sed -n 's/^upstream_base: *\([0-9a-f]\{7,\}\).*/\1/p' doc/PRUNING.md)
git rebase --onto <new-upstream-sha> "$base" dev
# then update upstream_base in doc/PRUNING.md, and squash the verified batch onto main
```

Completion: no conflict markers (`git diff --check` clean), each conflict has a recorded reason, and each ported commit has its upstream SHAs noted for the ledger.

## 5. Ledger

Append one line per ported feature to `doc/sync/ledger.md`: upstream SHA set → fork commit SHA → feature name.

Without this line a later big sync cannot tell a ported feature from a missing one, and the feature ends up in the tree twice.

Completion: the ledger has an entry for every commit this session created.

## 6. Seam review

Walk `doc/seams.md`. For each modified upstream file, confirm the fork's change survived and its `Depends on` symbols still resolve; for each added fork tree, confirm nothing upstream now duplicates it.

Completion: every entry has a verdict — kept / overwritten upstream / needs-human.

## 7. Docs

- `doc/FUNCTION_MAP.md`: follow what upstream actually became. Rewriting a section wholesale is fine; it describes upstream.
- `doc/PRUNING.md`: **verify only, never re-decide.** Confirm `removed_paths` still hold and the anchors are accurate. Report discrepancies rather than silently editing.

Completion: both documents match the code; discrepancies are listed.

## 8. Gates

Invoke the `breeze-sync-gates` skill. Run it here rather than relying on memory.

Completion: all four gates green, or stopped at the first red one with a located cause.

## 9. Report

Write `doc/sync/<date>/report.md`: what was ported, what was skipped, UAPI decisions, items needing a human, gate results.

Completion: the report is written and its "needs human" list is either empty or explicit.

## Stop rules

Stop and ask, without improvising:

- Upstream touched `uapi/` — UAPI commits are always a human decision (three layers ship together).
- G1 or G2 is red.
- A commit inside a removed area, where you cannot tell whether the deleted file is subject or bystander.
- A conflict that `reference/conflict-policy.md` does not resolve.
- A big sync is being considered but no human asked for one.

When stopping, state: which step, what matched, both versions, your recommendation.
