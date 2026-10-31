#!/usr/bin/env node
/**
 * Gate: enforce the five-layer acyclic Manager package graph.
 *
 * The layers are a funnel, not a bag of packages: a file may only import the
 * layers below its own, so a change in a page can never force a change in the
 * component library, and the composition root can never leak into a screen.
 *
 *   Layer 1  data/ core/ magica/ profile/ + KernelSUApplication.kt Natives.kt Kernels.kt
 *   Layer 2  ui/theme/ ui/icons/ ui/util/
 *   Layer 3  ui/component/{common,breeze}/
 *   Layer 4  ui/nav/ ui/animation/
 *   Layer 5  ui/screen/ and the sibling host ui/webui/
 *
 * Zero exemptions: every Kotlin file under SOURCE_ROOT is classified by its
 * path, and a path the graph does not name is either the composition root
 * (`ui/BreezeApp.kt`, `ui/BreezeShell.kt`, `ui/MainActivity.kt`) or a
 * violation in its own right. A new package that no layer claims therefore
 * fails the gate instead of silently importing anything it likes.
 *
 * Reason strings are English, matching DOC_LANG in config.mjs.
 */
import { finish, lines, walk } from './_shared.mjs'

const SOURCE_ROOT = 'manager/app/src/main/java/me/weishu/kernelsu'

/** Line-anchored, so `// import ...` and ` * import ...` comment lines never match. */
const IMPORT = /^\s*import\s+([A-Za-z0-9_.*]+)/

/** `ui/BreezeApp.kt` / `ui/BreezeShell.kt` / `ui/MainActivity.kt` own the composition root. */
const UI_ROOT = /^me\.weishu\.kernelsu\.ui\.[A-Za-z0-9_*]+$/
const MATERIAL3 = /^androidx\.compose\.material3(?:\.|$)/
const COMPOSE = /^androidx\.compose\./

/** `test` receives the imported FQN; `reason` is the violation cause. */
const banPackage = (pkg, reason) => ({
  test: (fqn) => fqn === pkg || fqn.startsWith(`${pkg}.`),
  reason,
})

const banPattern = (pattern, reason) => ({
  test: (fqn) => pattern.test(fqn),
  reason,
})

const UI_ROOT_BAN = banPattern(UI_ROOT, 'lower layers must not reach the ui composition root')

/** Layer 1 (data|core|magica|profile + root files): pure Kotlin, so no UI types and no Compose. */
const DATA_BANS = [
  banPackage('me.weishu.kernelsu.ui', 'Layer 1 must not depend on the UI layer'),
  banPattern(COMPOSE, 'Layer 1 must not depend on Compose'),
]

/** Layer 2 (ui/theme|icons|util/): may only reach Layer 1 and its own group. */
const SHARED_BANS = [
  banPackage('me.weishu.kernelsu.ui.component', 'Layer 2 must not depend on the Layer 3 component library'),
  banPackage('me.weishu.kernelsu.ui.nav', 'Layer 2 must not depend on the Layer 4 navigation layer'),
  banPackage('me.weishu.kernelsu.ui.animation', 'Layer 2 must not depend on the Layer 4 transition layer'),
  banPackage('me.weishu.kernelsu.ui.screen', 'Layer 2 must not depend on the Layer 5 screen layer'),
  banPackage('me.weishu.kernelsu.ui.webui', 'Layer 2 must not depend on the Layer 5 WebUI host'),
  UI_ROOT_BAN,
]

/** Layer 3 (ui/component/): the component library never reaches data, routing or pages. */
const COMPONENT_BANS = [
  banPackage('me.weishu.kernelsu.data', 'Layer 3 must not depend on the Layer 1 data layer'),
  banPackage('me.weishu.kernelsu.core', 'Layer 3 must not depend on Layer 1 core'),
  banPackage('me.weishu.kernelsu.magica', 'Layer 3 must not depend on Layer 1 magica'),
  banPackage('me.weishu.kernelsu.profile', 'Layer 3 must not depend on Layer 1 profile'),
  banPackage('me.weishu.kernelsu.Natives', 'Layer 3 must not depend on the Natives JNI facade'),
  banPackage('me.weishu.kernelsu.Kernels', 'Layer 3 must not depend on Kernels'),
  banPackage(
    'me.weishu.kernelsu.KernelSUApplication',
    'Layer 3 must not depend on the Application singleton',
  ),
  banPackage('me.weishu.kernelsu.ksuApp', 'Layer 3 must not depend on the Application singleton'),
  banPackage('me.weishu.kernelsu.ui.nav', 'Layer 3 must not depend on the Layer 4 navigation layer'),
  banPackage('me.weishu.kernelsu.ui.animation', 'Layer 3 must not depend on the Layer 4 transition layer'),
  banPackage('me.weishu.kernelsu.ui.screen', 'Layer 3 must not depend on the Layer 5 screen layer'),
  banPackage('me.weishu.kernelsu.ui.webui', 'Layer 3 must not depend on the Layer 5 WebUI host'),
  UI_ROOT_BAN,
]

/** `common/` is the skin-agnostic base, so it must not reach the Breeze design system. */
const COMMON_BANS = [
  banPackage(
    'me.weishu.kernelsu.ui.component.breeze',
    'ui/component/common must not depend on the breeze skin in the same layer',
  ),
  ...COMPONENT_BANS,
]

/** Layer 4 (ui/nav|animation/): routing must not know page implementations. */
const NAV_BANS = [
  banPackage('me.weishu.kernelsu.ui.screen', 'Layer 4 must not depend on the Layer 5 screen layer'),
  banPackage('me.weishu.kernelsu.ui.webui', 'Layer 4 must not depend on the Layer 5 WebUI host'),
  UI_ROOT_BAN,
]

/** Layer 5 (ui/screen/, ui/webui/): the two sibling hosts never reach into each other. */
const SCREEN_BANS = [
  banPackage('me.weishu.kernelsu.ui.webui', 'ui/screen must not depend on the ui/webui host in the same layer'),
]
const WEBUI_BANS = [
  banPackage('me.weishu.kernelsu.ui.screen', 'ui/webui must not depend on the ui/screen layer in the same layer'),
]

/** Layer 5 state/entry files: business logic stays free of the component library and Material3. */
const STATE_ENTRY_BANS = [
  banPackage(
    'me.weishu.kernelsu.ui.component',
    '*ViewModel/*UiState/*Entry must not depend on the Layer 3 component library',
  ),
  banPattern(MATERIAL3, '*ViewModel/*UiState/*Entry must not depend on Material3'),
]

/** A component file lives in exactly one of the two Layer 3 groups. */
const COMPONENT_GROUPED = /^ui\/component\/(?:common|breeze)\//

/** Layer 1 also owns the three root files; they sit beside `ui/`, not inside it. */
const ROOT_FILES = new Set(['KernelSUApplication.kt', 'Natives.kt', 'Kernels.kt'])

/**
 * The composition root: the only files allowed to sit directly under `ui/`.
 * They are above the funnel rather than inside it, so no layer bans apply; the
 * paths are listed rather than pattern-matched so a new `ui/*.kt` file is a
 * failure rather than a quiet promotion to the root.
 */
const UI_ROOT_FILES = new Set([
  'ui/BreezeApp.kt',
  'ui/BreezeShell.kt',
  'ui/MainActivity.kt',
])

const failures = []
let scanned = 0

for (const relPath of walk(SOURCE_ROOT, (p) => p.endsWith('.kt'))) {
  scanned += 1
  const underSource = relPath.slice(SOURCE_ROOT.length + 1)
  const basename = underSource.split('/').at(-1)

  // The Activity-viewmodel package was dissolved in F5; a file here re-opens it.
  if (underSource.startsWith('ui/viewmodel/')) {
    failures.push(
      `${relPath}: ui/viewmodel/ was deleted; a ViewModel lives beside the page it serves`,
    )
    continue
  }

  let bans = []
  if (
    /^(?:data|core|magica|profile)\//.test(underSource) ||
    ROOT_FILES.has(underSource)
  ) {
    bans = DATA_BANS
  } else if (/^ui\/(?:theme|icons|util)\//.test(underSource)) {
    bans = SHARED_BANS
  } else if (underSource.startsWith('ui/component/')) {
    if (!COMPONENT_GROUPED.test(underSource)) {
      failures.push(
        `${relPath}: a component file belongs under ui/component/common/ or ui/component/breeze/`,
      )
    }
    bans = underSource.startsWith('ui/component/common/')
      ? COMMON_BANS
      : COMPONENT_BANS
  } else if (/^ui\/(?:nav|animation)\//.test(underSource)) {
    bans = NAV_BANS
  } else if (underSource.startsWith('ui/screen/')) {
    bans = /(?:ViewModel|UiState|Entry)\.kt$/.test(basename)
      ? [...SCREEN_BANS, ...STATE_ENTRY_BANS]
      : SCREEN_BANS
  } else if (underSource.startsWith('ui/webui/')) {
    bans = WEBUI_BANS
  } else if (UI_ROOT_FILES.has(underSource)) {
    // The composition root sits above the funnel: no layer may import it, and
    // it may import anything, so it carries no bans of its own.
    continue
  } else {
    // No layer claims this path. It must not fall through with no bans: an
    // unclassified file would be free to import across every boundary above.
    failures.push(
      `${relPath}: unclassified path outside the five-layer graph`,
    )
    continue
  }

  if (bans.length === 0) continue

  lines(relPath).forEach((line, index) => {
    const match = IMPORT.exec(line)
    if (!match) return
    const ban = bans.find((candidate) => candidate.test(match[1]))
    if (ban) failures.push(`${relPath}:${index + 1} — ${ban.reason}: ${match[1]}`)
  })
}

finish(
  'verify-arch-layers',
  failures,
  `${scanned} Kotlin files respect the five-layer one-way dependency graph`,
)
