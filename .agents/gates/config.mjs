/**
 * Project slot: every value the gates read that depends on this repository.
 * The gate scripts are preset copies, so project values go here, never there.
 *
 * Budgets are ceilings in counting units and live here alone: a second copy in
 * prose drifts the moment either side is edited.
 */

/* ── Scope ──────────────────────────────────────────────────────────────── */

/**
 * Paths owned by the framework. `verify-detachable` fails when a project file
 * names one of them. Do NOT add a directory the project also uses: the
 * framework cannot both own that name and stay deletable.
 */
export const MANAGED_PATHS = ['AGENTS.md', '.agents/']

/** Where module-level constraints live, relative to the repository root. */
export const ARCHITECTURE_DIR = '.agents/architecture'

/* ── Choices recorded at init ───────────────────────────────────────────── */

/**
 * Documentation language: 'zh', 'en', or 'both'.
 * Drives which heading names the gates look for; gates stay neutral otherwise.
 */
export const DOC_LANG = 'en'

/**
 * Agent Note lifecycle: 'four-state', 'two-state', or 'disabled' (no notes
 * tree; the note gates report a skip). Off here because the decision record
 * lives in `doc/decisions/`, which must survive deleting `.agents/`.
 */
export const NOTE_MODE = 'disabled'

/**
 * How the framework is kept out of the project's way:
 *   'stealth' — managed paths go into .git/info/exclude, nothing enters the repo
 *   'team'    — managed paths are committed; a CI workflow may enforce the gates
 */
export const SEPARATION = 'team'

/**
 * Optional CI enforcement. `null` means the gates run locally only.
 */
export const CI = null

/**
 * Project files allowed to mention managed paths without CI, such as a hook
 * that calls the gates. Each still needs a guard.
 */
export const FRAMEWORK_AWARE_FILES = []

/** How to inspect the publish surface, or `null`: no package tarball here. */
export const PACK_CHECK = null

/** Gates that are switched on. `verify-all` runs exactly these. */
/*
 * `verify-doc-entries` is a project-owned gate, not a preset one: the fork
 * ledger lives in `doc/`, which the preset gates deliberately do not govern.
 */
export const ENABLED_GATES = [
  'verify-agent-note-format',
  'verify-agent-skills',
  'verify-arch-layers',
  'verify-context-cost',
  'verify-detachable',
  'verify-doc-budgets',
  'verify-doc-entries',
  'verify-human-docs',
  'verify-md-links',
  'verify-scaffold',
]

/* ── Ignored paths ──────────────────────────────────────────────────────── */

/**
 * Directories every walk skips. `lib/` is NOT here: it is a source directory in
 * Dart, Elixir and Ruby, so a project that builds into it adds it itself.
 */
export const IGNORE_DIRS = [
  'node_modules',
  '.git',
  'build',
  '.gradle',
  '.kotlin',
  '.idea',
  '.cxx',
  'dist',
  '.pnpm-store',
  '.venv',
  '__pycache__',
  'target',
  'vendor',
  '.next',
]

/* ── Budgets, in counting units ─────────────────────────────────────────── */

/** Exact-path budgets. */
export const FILE_BUDGETS = [
  { path: 'AGENTS.md', limit: 600 },
  { path: '.agents/README.md', limit: 400 },
]

/** Every module document under ARCHITECTURE_DIR shares this ceiling. */
export const ARCHITECTURE_BUDGET = 600

/** Every Agent Note shares this ceiling. */
export const NOTE_BUDGET = 1500

/** Every project skill shares this ceiling. */
export const SKILL_BUDGET = 800

/** Preset skills are not always-loaded, so their ceiling only stops growth. */
export const PRESET_SKILL_BUDGET = 1600

/**
 * The untracked personal overlay. Its own pool so personal habits cannot push
 * the team ratchet, still capped because the host injects it every turn.
 */
export const LOCAL_BUDGET = 250

/** Warn once a document is this close to its ceiling. */
export const WARN_RATIO = 0.9

/* ── Always-loaded chain ────────────────────────────────────────────────── */

/** The always-loaded chain the host injects, root first. */
export const CONTEXT_CHAIN = [
  'AGENTS.md',
  '.agents/AGENTS.md',
  '.agents/notes/AGENTS.md',
  '.agents/notes/implemented/AGENTS.md',
  '.agents/skills/AGENTS.md',
  '.agents/architecture/AGENTS.md',
]

/** The personal overlay. Outside the chain on purpose: printed, never counted. */
export const LOCAL_OVERLAY = 'AGENTS.local.md'

/**
 * Measured chain baseline, in counting units. A ratchet, not a target: raise it
 * only with a deliberate budget decision, never to make a red run green.
 */
export const CONTEXT_BASELINE = 600

/* ── Structure ──────────────────────────────────────────────────────────── */

/** The only H2 sections the root entry file may carry, in this order. */
export const ROOT_SECTIONS = [
  'What this repo is',
  'Directory map',
  'Command matrix',
  'Where to look next',
  'Three rules',
]

/** H2 the navigation table sits under. */
export const NAV_SECTION = 'Directory map'

/**
 * Human-facing map. `null` means the project has no separate map and the module
 * documents carry the whole story.
 */
export const HUMAN_MAP = null
