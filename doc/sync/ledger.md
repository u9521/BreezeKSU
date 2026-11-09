# Sync ledger

One line per ported feature. This is what stops a later big sync from porting the same thing twice.

## Why it exists

A feature sync lands upstream code as a fork commit with a hand-edited diff. Months later, a big sync rebases onto a newer upstream tip — and that tip already contains the feature. `git rebase` discards a plain cherry-pick by patch-id, but a hand-merged port has different text, so it is not recognised, and the tree ends up with two implementations of one feature. Both usually compile; the bug only shows when they disagree.

The ledger is the record that closes that gap: before replaying, remove every upstream SHA set listed here from the replay list.

## Format

```
<upstream SHA set> | <fork commit subject> | <feature>
```

- **Upstream SHA set** — every commit whose behaviour the port carries, comma-separated. A feature often spans several commits.
- **Fork commit subject** — the subject of the commit on `dev` that landed it, not its SHA. A squash onto `main`, or any rebase, rewrites every SHA; the subject survives both, and `git log --grep` finds it. A SHA here would go stale exactly when the ledger is needed.
- **Feature** — a short name a human can match against a commit subject.

## Entries

| Upstream SHAs | Fork commit | Feature |
| :--- | :--- | :--- |
| `8b88319,a45fd1a,3b0f3e3,8ac9a44` (repository `breezexample`, not `tiann/KernelSU`) | `manager: follow the template's overlay backdrop rework` | overlay backdrop: implementation slot + shaped progress |
| `ae526bf,c7bfed8,cbd6c66` (repository `breezexample`) | `manager: follow the template's split scene rework` | split scene: pane markers, one strip under a moving cursor, ratio source |
| `730f6446` (`tiann/KernelSU`) | `manager: rebuild on Breeze, prune three features and port the WebUI host` | x-zip MIME types, re-expressed in `BreezeIntentRouter` because upstream's dispatcher is a pruned path |

## Maintaining it

- `breeze-upstream-sync` step 5 appends a row; a port that lands without a row is an incomplete port.
- A big sync reads the whole table before replaying and drops the listed commits from the work queue.
- Never delete a row when a feature is later pruned: the pruning table records the removal, and the ledger records that the code came from upstream.
