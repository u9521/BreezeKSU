---
status: prune
mechanism: block delete
residue-rev: 1
---

# F1 — one renderer: Breeze only

Removes upstream's two interchangeable Manager UIs (Material and Miuix) and leaves Breeze
as the only one. Maps to FUNCTION_MAP §5 coupling 11 ("Two Manager UIs") and consumes
decision §12 ("The Breeze skin").

## Why

Every screen upstream ships existed twice, and the fork's Breeze skin made it three — no,
worse: Breeze existed *beside* the pair, so the app carried roughly three times the UI code
needed to render one interface. The `ui_mode` preference chose between them, which meant a
bug in a screen existed in up to three implementations and every upstream UI change had to be
replayed against code the fork never ran.

The fork ships Breeze. Keeping the other two meant paying upstream's UI maintenance three
times over to run one of them, and keeping a second settings surface (the UI-style picker)
whose only job was to choose between renderers that no longer both existed.

The Material Icons dependency goes with it: 29 icons pulled in an artifact containing every
Material icon ever published, while the fork already generates its own Material Symbols set
from a manifest.

## Surface

```
kernel/           — none
userspace/ksud/   — none
manager/          — removed: *Miuix.kt (27), *Material.kt (25), ui/component/miuix/ (17),
                    MiuixTheme.kt, ui/navigation3/ (3), and the androidx material-icons-extended
                    dependency; with the upstream pages gone, so went the features that only they
                    reached: data/script/ (5), ScriptRun* ViewModels and state,
                    runOneshotScript/removeAsRoot, and the whole module repository: all of
                    data/registry/ (RegistryClient, RegistryParser, RegistryModels, RegistryUrls,
                    RegistryUpdatePolicy, DownloadIntegrity, FileRegistryCache, RegistryLocale),
                    ModuleRepoRepository[Impl], RepoModule, ui/screen/modulerepo/, the verified-
                    download path in DownloadService/DownloadManager, and the registry-restricted
                    renderer in the markdown components
                  — added: ui/component/bottombar/BreezeNavBadges.kt, ui/theme/BreezeTheme.kt,
                    ui/screen/sulog/SulogFormatting.kt, 24 Material Symbols entries,
                    src/screenshotTest/ (11 previews incl. IconGalleryPreview)
                  — moved: breezeui/ → ui/ (155 files), previews main → screenshotTest
uapi/             — none
build / CI        — libs.versions.toml drops miuix (5 artifacts) and material-icons-extended;
                    gradle.properties gains android.experimental.enableScreenshotTest
```

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no kernel ioctl is involved; `grep -rn 'KSU_' manager/` unchanged |
| 2 Feature ids | not applicable | `KSU_FEATURE_MAX` untouched |
| 3 Strings | removed `settings_ui_mode`/`_summary`, 36 `script_*` and 21 `module_repo_*` keys | deleted from both `values/` and `values-zh-rCN/`; `module_repo_script_badge` went too. Two `module_repo_*` keys stay because the download pipeline uses them for verification errors. The other locales still carry the rest as unused translations, which is harmless |
| 4 Manifest / gradle | removed 5 miuix artifacts, material-icons-extended, the `overrideLibrary` line | `grep -rn 'miuix\|material-icons' manager/app/build.gradle.kts manager/gradle/libs.versions.toml` returns nothing |
| 5 Breeze pages | none removed; both features that lost their last entry point were pruned | every ui/screen/<feature>/ pair survives and the icon gallery is *added*. Module repository (M5) and one-shot scripts (M7) had **no Breeze page**, so once the upstream pages went they were unreachable, and neither is left half-alive: the registry browser, the verified-download path it drove and the restricted renderer are all gone. A module update is now the updateJson URL the module declares, downloaded like any other file |
| 6 supercall wrappers | not applicable | `ksucalls.rs` and `ksu.cc` untouched |
| 7 ksud CLI | not applicable | no subcommand changes |
| 8 Module parameters | not applicable | bootconfig keys untouched |
| 9 Build workflows | new source set | `src/screenshotTest` needs `android.experimental.enableScreenshotTest=true`; not part of any APK |
| 10 Seams | S1 rewritten, S2 deleted, S1's dependants corrected | see [seams.md](../seams.md) rows 4–6 and Part 2 |

## Verified

Landed inside the batch squash rather than as a commit of its own. The whole Manager rewrite —
one renderer, the Compose markdown renderer, the overlay host and the five-layer funnel — is a
single commit, so an upstream change to `manager/` needs one resolution point instead of one
per step. It was developed in segments and every segment compiled on its own, which is why the
entry boundaries fall where they do — the first two segments could not be split along the
Miuix/Material line, since `MainActivity` and `navigation3/Navigator.kt` were built on
miuix-nav and disappear with it.

```
manager: rebuild the Manager on a five-layer Breeze-only architecture
           :app:assembleDebug                    BUILD SUCCESSFUL (each segment, before the squash)
           :app:compileDebugScreenshotTestKotlin BUILD SUCCESSFUL
```

Generator idempotency: running `fetch_material_icons.py` twice produced no diff.

Net effect against the baseline: 380 files changed, +2880 / −32518.

Device run: done. The five main pages, the sub-screens reachable from them and the module
WebUI were exercised on a device after the batch, which is what clears the icon swap — the
glyph weight change (androidx Filled vs Material Symbols ROUNDED FILL=1) is visible but does
not break any layout. The build alone would not have proven that.

## Sync risk

This is the largest divergence from upstream the fork has made, and it is *structural* rather
than additive: upstream will keep shipping `*Material.kt` and `*Miuix.kt` for every new screen,
and every future upstream UI change lands in files that no longer exist here.

- A ported upstream feature that touches a screen must be re-expressed as a Breeze page, not
  copied. The `breeze-upstream-sync` skill's conflict-precedence rule applies: the fork's UI
  wins, upstream's logic is lifted.
- `ui/component/expressive/` is upstream's `component/material/` renamed. If upstream adds a
  component to that package, it arrives under a path that does not exist; the file is added,
  not merged, and its callers are upstream screens that are gone.
- `ui/component/overlay/` has no upstream counterpart: `BreezeOverlay`, `BreezeAlertDialog`, and
  `BreezeOverlaySheet` host modals in a shared full-screen window. `BreezeAlertDialog` mirrors
  Material3's `internal` `AlertDialogContent`/`AlertDialogImpl` tokens (at the bottom of
  `BreezeAlertDialog.kt`). `BreezeOverlaySheet` no longer copies `internal` `SheetWindowInsets`:
  the copy read the sheet's live offset during measurement, a layout feedback loop that made the
  sheet tremble while an inner list scrolled, so both it and its `consumeWindowInsets` are gone.
- `MainActivity` is now ~55 lines and no longer resembles upstream's. Treat it as fork-owned
  rather than as a seam — a three-way apply against upstream's version is not meaningful.
- Upstream's `ui_mode` preference is still read by `SuFilePathHandler`; that call site keeps
  working because the value is still `breeze`.

## Rollback

There is no F1-sized range to revert: the Manager rewrite is a single commit, and `git revert`
takes it whole. Reversing only this pruning means reversing that commit's slice for the deleted
UI trees:

```
git show "$(git log --grep='^manager: rebuild the Manager' -1 --format=%H)" -- manager/app/src/main/java/me/weishu/kernelsu/ui/ | git apply -R
```

Nothing here is ABI state: no ioctl number, feature id or on-disk format changed, so a revert
restores the old tree exactly. Two things do not come back for free:

- An install that ran with this build has `ui_mode=breeze` written by `BreezeMode`'s init
  block, and its `miuix_monet` key is gone. Both are preferences with defaults; they re-derive.
- The 24 generated icon files are regenerable from `icons.toml`, so a revert that keeps the
  manifest still works.
