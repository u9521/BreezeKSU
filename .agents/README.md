# The agent framework in this repo

Self-description of the framework, not an expectations list. What a repository *should* have is defined by the engineering preset; this file records only what a scan cannot infer.

## Exemptions

<!-- repo:begin -->

Initialised: 2026-10-23 · Language: en

Deliberately not done here, relative to the preset defaults:

- **No Agent Note tree.** Decisions are recorded in `doc/decisions/`, which belongs to the fork and must survive deleting `.agents/`. `NOTE_MODE` is `disabled`; the note gates report a skip.
- **No CI enforcement.** `.agents/gates/verify-all.mjs` runs locally; the preset's CI templates were not installed.
- **No `docs/MAP.md`.** The architecture pages carry the whole story.
- **Gate checks for the sync lane are a skill, not a script.** `breeze-sync-gates` runs the upstream-sync checks (pruning integrity, UAPI agreement, compiles); the preset's `verify-*` gates cover documentation and scaffold only.

<!-- repo:end -->

## Conventions

- Slot markers (`repo:begin` / `repo:end`) delimit repository-owned content: updates rewrite everything outside the slots and keep the inside verbatim.
- Preferences derived by scanning (language, ignore rules, existing docs) are not recorded here.
- Only "the user explicitly asked not to do this" is recorded, because it is indistinguishable afterwards from "the preset never generated it".

## Personal overlay

Create `AGENTS.local.md` in the workspace root. The host injects it alongside `AGENTS.md`, the more specific file wins, and it is ignored by git.

It may hold interaction habits and reporting format. It may not waive gates, the note-state contract, or module constraints. It has its own budget, is excluded from the team baseline, and `audit-doc-slop` flags overreach without blocking.

## Detachability

`AGENTS.md` and `.agents/` are managed by the framework: deleting them must leave a repository that still installs, builds and ships, so project files must not name them. `verify-detachable` enforces that direction. The reverse is allowed and expected — the framework reads the project.

## Gates

One entry point: `node .agents/gates/verify-all.mjs`. All thresholds live in `.agents/gates/config.mjs`; no document restates them.
