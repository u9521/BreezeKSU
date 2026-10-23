---
name: commit
description: Use when preparing to commit changes in this repository — deciding commit granularity, writing the message, handling paths that need authorisation, and confirming nothing unrelated slipped in. Triggered by "commit", "land this", or a question about whether a change needs asking first.
---

# Commit

A commit is the only action that moves work out of the working tree, so this covers **boundaries**: what one commit contains, how the message reads, and which changes must ask first.

## One thing at a time

One commit answers one question. Refactors and behaviour changes are separate; formatting and logic are separate; mechanical rewrites and design decisions are separate.

Split by the **intent** of the diff, not by file count: one intent touching 26 files is still one commit; two intents colliding in one file need two.

## Message

- Subject in the imperative, describing what the code is like afterwards, in upstream's `<scope>: <summary>` form — a short lowercase scope for the touched area, then a concise sentence-case summary with no trailing period:

  ```
  manager: drop module registry
  ksud: drop boot-patch-v2
  docs: record the pruning of M5, M7
  ```

- Body after a blank line: why, what was rejected, what it costs. **Reasoning lives in the body, never in the subject.**
- Keep the subject under about 72 characters; append `(#1234)` when referencing an upstream PR.
- Glance at `git log --oneline` before writing, to stay consistent with the scopes and capitalisation in use.

## Ported code carries its author

A commit that ports upstream code adds trailers after the body, one per distinct upstream author of that code, copied verbatim:

```bash
git show -s --format='%an <%ae>' <sha>
```

```
Co-authored-by: 5ec1cff <56485584+5ec1cff@users.noreply.github.com>
```

The trailer credits whoever wrote the code upstream — that is its entire purpose, so **no tool, assistant, bot or session ever appears in it**. Fork-owned work that ports nothing (documentation, branding, `breezeui/` skin) carries no trailer.

## Branch flow

Work commits on `dev`. `main` is the **landing line**, not a second working branch: once a batch on `dev` is verified, squash it onto `main` as one finished change.

```bash
git checkout dev            # work here
# ... commits, checks green ...
git checkout main
git merge --squash dev
git commit                  # one commit describing the finished batch
git checkout dev
git merge --ff-only main    # keep dev building on the landing line
```

Why squash rather than fast-forward: `main` should read as a sequence of completed changes a human can review one at a time, while `dev` keeps the working history — a port split into five commits, a fix reverted and redone, a pruning that took three attempts.

`main` never receives direct commits, and nothing is force-pushed to it.

## The fork ledger moves with the code

Three things are due in the **same commit** as the change they describe:

- **A pruning or a fork-owned feature** gets an entry under `doc/entries/`, an index row in `doc/PRUNING.md`, and — for a prune — the matching `removed_paths` / `removed_symbols` entry in its anchor block. Skipping this is how a feature comes back months later: the sync has nothing to check against.
- **A modified upstream file** gets a row in `doc/seams.md`, or an update to the existing one. "Modify freely" is only safe because this record exists.
- **A changed decision** edits the section it lives in under `doc/decisions/`.

`node .agents/gates/verify-doc-entries.mjs` checks that the entry files stay connected and well-formed; it cannot check that they are true — that is what `breeze-sync-gates` reviews.

Work that does not touch the fork's scope — a docs typo, a CI tweak, a dependency bump — owes none of this.

## Authorisation boundary

Free to change: `AGENTS.md`, `.agents/**`, `doc/**`.

Ask first — via `ask_user_question`, presenting the file, a diff summary and the reason, in one go — before touching:

<!-- repo:begin -->
- `kernel/**`, `userspace/**`, `manager/**`, `uapi/**` — source code
- `Cargo.toml`, `Cargo.lock`, `manager/gradle/libs.versions.toml` — manifests and lockfiles
- `manager/**/build.gradle.kts`, `.cargo/config.toml`, `justfile` — build configuration
- `.github/workflows/**` — CI
<!-- repo:end -->

The boundary is a division of labour, not a prohibition: the framework maintains the scaffold, day-to-day development belongs to whoever is building the project. When such a path genuinely must change, say why this time is different and wait for the answer.

## Before committing

1. `git status --short` for stray files: build output, temporaries, logs.
2. Run the checks this change actually needs — see the `pre-push-checks` skill.
3. Only `git add` and commit on green. Committing with a red gate hands the judgement to the next person.

## Change briefing for humans

When a delivery touches **≥5 source files** or crosses **≥2 module boundaries**, the report to the user (or the PR description) carries a briefing. Changes to 1–4 files are exempt.

1. **One sentence of substance**: how the system handled X before → how it handles X now.
2. **Layered reading order** (never alphabetical by filename): contract layer first (types, schemas, config, public interfaces), then the core layer read closely (where behaviour actually changed), then the mechanical layer skimmed (signature adjustments, import paths, routine tests) with a count and a reason.
3. **30-second observable check**: one or two commands a human can paste to see the behaviour change. "Tests are green" is not a substitute.

## Prohibited

- No `git push --force` to a protected branch; branches maintained by CI are never pushed by hand.
- No `--no-verify` to bypass hooks.
- No committing uncontrolled build artifacts or generated files.
