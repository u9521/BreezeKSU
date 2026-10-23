# Sync ledger

One line per ported feature. This is what stops a later big sync from porting the same thing twice.

## Why it exists

A feature sync lands upstream code as a fork commit with a hand-edited diff. Months later, a big sync rebases onto a newer upstream tip — and that tip already contains the feature. `git rebase` discards a plain cherry-pick by patch-id, but a hand-merged port has different text, so it is not recognised, and the tree ends up with two implementations of one feature. Both usually compile; the bug only shows when they disagree.

The ledger is the record that closes that gap: before replaying, remove every upstream SHA set listed here from the replay list.

## Format

```
<upstream SHA set> | <fork commit> | <feature>
```

- **Upstream SHA set** — every commit whose behaviour the port carries, comma-separated. A feature often spans several commits.
- **Fork commit** — the commit on `dev` that landed it.
- **Feature** — a short name a human can match against a commit subject.

## Entries

| Upstream SHAs | Fork commit | Feature |
| :--- | :--- | :--- |
| — | — | *(none ported yet; the fork delta so far is the Breeze UI, the branding, and documentation)* |

## Maintaining it

- `breeze-upstream-sync` step 5 appends a row; a port that lands without a row is an incomplete port.
- A big sync reads the whole table before replaying and drops the listed commits from the work queue.
- Never delete a row when a feature is later pruned: the pruning table records the removal, and the ledger records that the code came from upstream.
