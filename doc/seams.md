# Fork asset inventory

The contact surface between fork and upstream. Two parts, both checked during sync step 6 and cross-checked before every prune:

- **Modified upstream files** — files upstream owns where the fork has edits. An upstream rewrite can silently drop the edit.
- **Added fork trees** — files upstream does not have. The risk is not loss but duplication: upstream eventually grows something similar, and the tree ends up with both.

Re-derive against `upstream_base`, not against `main` — `main` carries fork documentation too, so it is not a clean upstream mirror:

```bash
base=$(sed -n 's/^upstream_base: *\([0-9a-f]\{7,\}\).*/\1/p' doc/PRUNING.md)
git diff --name-status "$base" dev | grep -v '^A'
```

Since F1 and F2 rewrote `ui/` around Breeze and the Compose markdown renderer, the raw diff against `upstream_base` lists ~200 non-added paths (mostly deleted upstream UI files). The table below records the actual contact surface that still matters during sync.

## Part 1 — modified upstream files

`Depends on` names the symbols the seam pulls in. It exists so a prune can grep one column instead of re-reading each file: if a pruning target appears there, the seam breaks and must be updated in the same change.

| # | File | What the fork changed | Depends on | When upstream overwrites it |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `kernel/Kbuild` | package name and signing constants | `KSU_PACKAGE_NAME` / `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH` | protected constants: keep the fork values |
| 2 | `manager/app/build.gradle.kts` | package/app name, the screenshot-test plugin, and dependencies (navigation3, haze + haze-blur, markdown) | `defaultManagerPackageName` / `defaultManagerName` | brand lines keep fork values; dependency lines accept upstream additions, keep both on conflict |
| 3 | `manager/settings.gradle.kts` | `rootProject.name` `KernelSU` → `BreezeKSU` | — | keep the fork name; upstream never touches this line |
| 3a | `manager/build.gradle.kts` | `androidCompileSdkVersionMinor = 1` added so the app can compile against a minor SDK level | the AGP `compileSdk { release(...) { minorApiLevel } }` block | keep the minor level; upstream's block accepts it as an addition |
| 4 | `manager/.../ui/MainActivity.kt` | **no longer a seam.** Upstream's two-renderer host was replaced by a ~60-line activity that calls `BreezeApp` unconditionally | `BreezeApp` in `ui/`, `BreezeIntentRouter` | upstream's version is not mergeable into this file; read it for behaviour, re-express in Breeze |
| 5 | `manager/.../screen/settings/SettingsMaterial.kt` | **deleted** — the file no longer exists | — | nothing to keep; an upstream edit to it is a signal that a screen was added that has no Breeze page |
| 6 | `manager/.../screen/settings/SettingsMiuix.kt` | **deleted** — same | — | same as row 5 |
| 7 | `manager/.../ui/webui/SuFilePathHandler.java` | `ui_mode` check accepts `"breeze"` | the `ui_mode` preference key | keep the fork's check |
| 8 | `manager/.../ui/webui/WebUIActivity.kt` | **no longer a seam.** The activity renders `BreezeWebUIRoot` unconditionally, and that root is a top-level sibling host built on `BreezeShell` | `BreezeWebUIRoot`, `BreezeShell` in `ui/` | read upstream's for behaviour, re-express |
| 9 | `manager/app/src/main/res/values/strings.xml` | the fork's own strings, added with no prefix: 29 keys the Breeze screens and the Compose markdown renderer read (`module_changelog_*`, `markdown_*`, `module_info_*`, `profile_template_*`, `select_all`, `search`, …) | every key in that set | both sides are additions: keep both. Match by key, not by a `breeze_*` prefix — the fork adds none |
| 10 | `manager/app/src/main/res/values-zh-rCN/strings.xml` | same | same as row 9 | keep both |
| 11 | `manager/gradle/libs.versions.toml` | `navigation3`, `haze`, `screenshot`, the Material Symbols split, and the markdown renderer in place of `commonmark` | `navigation3`, `haze`, `screenshot`, `markdown` | keep both |
| 12 | the repository's root agent entry file | rewritten wholesale as the fork's entry projection | the architecture pages and skills it links to | the fork owns this file; upstream edits are read for content, not applied |
| 13 | `.gitignore` | the fork's own ignore entries | — | merge by intent: upstream additions land, fork additions stay |
| 14 | `manager/.../data/repository/SettingsRepository(Impl).kt` | reshaped from a set of `var` preferences into one `StateFlow<UiSettings>` plus an atomic `update`; `uiMode` dropped | `UiSettings` in `data/`, every preference key name, the ksud feature calls | upstream's version is not mergeable; read it for a new key or feature call and re-express against `UiSettings` — keep the key verbatim |
| 15 | `manager/.../KernelSUApplication.kt` | hosts the four repository singletons, prefetches the app scan, and no longer implements `ViewModelStoreOwner` | `settingsRepository`, `superUserRepository`, `moduleRepository`, `templateRepository` | keep the singletons; an upstream `appViewModelStore` means a screen wants application scope — give it a repository instead |
| 16 | `manager/app/src/main/AndroidManifest.xml` | `DownloadService` declared at `.data.service` | the service's package | keep the attribute in step with the class; a stale name fails only at runtime |
| 17 | `manager/.../ui/component/breeze/dialog/Dialog.kt` | rewritten: the handle-and-continuation API (`ConfirmDialogVisuals`, `ConfirmDialogHandle`, `showConfirm`, `awaitConfirm`, the `Saver`) is gone; what remains is the `ConfirmBody` value and `ConfirmDialogState`/`LoadingDialogState` | `ConfirmBody` in `DialogBreeze.kt`, the call sites listed in the F2 entry | upstream's version is not mergeable into this file; read it for behaviour and re-express against `ConfirmBody` |

### Relocations (F3 and F5)

These upstream-managed files were **moved**, not pruned. They are recorded here rather than in
`removed_paths` for that reason: a path in that list is one upstream must never recreate, while a
relocated file is one the next sync must find at its new home. An upstream edit arriving at the old
path is applied to the new one, by hand.

| # | Old path (upstream) | New path (fork) | Depends on | On sync |
| :--- | :--- | :--- | :--- | :--- |
| R1 | `ui/KsuService.kt` | `data/service/KsuService.kt` | `ksuApp`, the service it starts | apply upstream's edit at the new path |
| R2 | `ui/util/DownloadService.kt` | `data/service/DownloadService.kt` | the `DownloadService` `<service>` declaration in `AndroidManifest.xml` | the manifest attribute follows the class package; a stale name fails only at runtime |
| R3 | `ui/util/DownloadManager.kt`, `ui/util/Downloader.kt` | `data/service/` | `DownloadService`, the network layer | same |
| R4 | `ui/util/{DeviceNameHelper,KsuCli,LogEvent,Network,OemHelper,SELinuxChecker,SulogHelper,UidGroupUtils}.kt`, `ui/util/module/ModuleRepoApi.kt` | `data/ksu/` | `Natives`, the ksud CLI, each other | these are Layer 1 now: an upstream edit adding a Compose import breaks the arch gate, not just the layering |
| R5 | `ui/viewmodel/{Home,Module,Settings,Sulog,SuperUser,Template}ViewModel.kt` | `ui/screen/<feature>/*ViewModel.kt` | the page's `*UiState`/`*Utils`, the repositories | a rename only in F5; the bodies are the upstream ones plus F3/F5 edits. Upstream adding a seventh ViewModel to `ui/viewmodel/` is rejected outright by the arch gate |
| R6 | `ui/util/module/Shortcut.kt` | `ui/screen/module/Shortcut.kt` | `ModuleViewModel`, the launcher-shortcut API | the module page owns its shortcuts now |
| R7 | `ui/theme/Theme.kt` | `ui/theme/BreezeTheme.kt` (**superseding**, so the old path is also in `removed_paths`) | `ColorMode` (now `data/UiSettings.kt`), `BreezeTheme`, `LocalEnableBlur`, `rememberBreezeColorScheme` | a rewrite, not a move: upstream's `UiMode`/`LocalUiMode` reads are gone. Read upstream's for a new palette knob and re-express against `UiSettings` |
| R8 | `ui/util/ModuleParser.kt` | `data/ksu/ModuleParser.kt` | `Module`, `R.string.*` from the `prop` parse errors | apply upstream's edit at the new path. It is Layer 1 now, so an added Compose import fails the arch gate instead of only breaking the layering |
| R9 | `ui/util/SeContextValidator.kt` | `data/ksu/SeContextValidator.kt` | nothing — a pure validator over the `user:role:type[:range]` string | same as R8 |
| R10 | `ui/util/module/LatestVersionInfo.kt` | `data/model/LatestVersionInfo.kt` | nothing — a data class the update check deserialises into | same as R8 |

One entry in `removed_paths` is really "superseded" rather than "moved": the fork deleted the file
and wrote a replacement, so both the old path and the new file exist. `ui/theme/Theme.kt` became
`ui/theme/BreezeTheme.kt`. The old path is listed so a restored copy is caught.

## Part 2 — added fork trees

| Path | What it is | Depends on | Duplication risk during sync |
| :--- | :--- | :--- | :--- |
| `manager/app/src/main/java/me/weishu/kernelsu/ui/` | the whole Manager UI, in five enforced layers: `theme`/`icons`/`util` → `component/{common,breeze}` → `nav`/`animation` → `screen/<feature>`/`webui`, with `ui/BreezeApp.kt` the composition root and `ui/BreezeShell.kt` the skin shell above them. Each page is a `*ViewModel`/`*UiState` → `*Entry` → `*Breeze` funnel; `ui/viewmodel/` no longer exists. `verify-arch-layers.mjs` is the executable copy of this row | `SettingsRepository` keys, `Natives.Profile`, each page's `*UiState` | upstream never touches it. Since F1 there is no second renderer to collide with, so the risk is only that an upstream feature arrives with logic the fork must lift into a Breeze page. An upstream file under `ui/component/` also needs classifying into `common/` or `breeze/` before it compiles into this tree |
| `manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/markdown/` | a Compose markdown renderer (`MarkdownContent.kt` rewritten in F2) and the `style/` preset (`BreezeMarkdownStyle.Default`) that configures it | `com.mikepenz:multiplatform-markdown-renderer` 0.46.0 (its model interfaces and `MarkdownListItems`) | upstream keeps its own WebView renderer at a path the fork has deleted, so an upstream markdown change arrives as an addition to files that are gone — not to `assets/webview/` as a whole, since `download.js` there serves Module WebUI and the fork keeps it. A renderer major-version bump renames model interfaces; the style type holds the library's own bundles so the compiler points at every landing site |
| `manager/app/src/main/java/me/weishu/kernelsu/ui/icons/` | generated Material Symbols `ImageVector`s plus the hand-written `MaterialSymbol.kt` | `icons.toml` and `fetch_material_icons.py` | a regenerated file must match what the manifest declares; never hand-edit |
| `manager/app/src/screenshotTest/` | Compose previews, including `IconGalleryPreview`. Not compiled into any APK | the `screenshotTest` source set and `android.experimental.enableScreenshotTest` | none; but a preview that stops compiling breaks `:app:compileDebugScreenshotTestKotlin`, not the app build |
| `doc/` | the fork ledger: handoff, pruning table, entries, function map, glossary, decisions, sync records | nothing — pure documents | none; upstream has no `doc/`, and its `docs/` holds translated READMEs only |
| the agent framework directory | skills, architecture pages, gates | the ledger under `doc/` | none; upstream carries only a root agent entry file |

## Pruning cross-check

Before deleting anything, walk Part 1 and Part 2 and ask of each row: **does this seam reference a symbol I am about to remove?**

The answer is not obvious from the table alone, which is why `Depends on` exists:

```bash
# for each symbol S being pruned, and each path P in this inventory:
rg -n "$S" "$P"
```

A hit means the seam breaks when the target goes. It belongs in the pruning entry's residue table with the fix spelled out — usually "drop that import and its route", occasionally "relocate the seam". Leaving it to the compiler only catches the Kotlin half; a `Kbuild` constant or a `strings` key fails silently.

After the prune, update the affected rows here: a seam whose dependency was removed is no longer "keep the seam" but "seam removed" or "seam relocated to X".

## Why there are so few seams

Until F1 the fork avoided adding a constant to upstream's `UiMode` enum, and selected its skin
through the `ui_mode` string preference instead. That constraint is gone: the enum was deleted in
F4, and `ui/BreezeShell.kt` is now the only place a skin is named. The `ui_mode` key survives in
exactly one place — `SuFilePathHandler` still reads it and still expects `"breeze"` — because an
installed app written by an older build carries the value forward.

The remaining modification list is short for a simpler reason: fork features live in files
upstream does not have. It is a property worth preserving, but it is not a rule — the fork may
modify upstream files freely when that is the clearer change. What it may not do is modify one
without recording it here, because the next sync would then have no way to notice the edit
vanishing.

## When upstream rewrites a seam

If upstream restructures a function containing a seam, relocate the seam into the new structure rather than reverting upstream's refactor. A reverted refactor creates a conflict that repeats every sync; a relocated seam costs one edit.
