---
status: modify
mechanism: swap
residue-rev: 1
---

# F4 — BreezeApp composition root and a pure navigation layer

Replaces `BreezeMain` and the Activity ViewModel pair with a composition root (`ui/BreezeApp.kt`)
plus a skin shell (`ui/BreezeShell.kt`), splits the tab host into a skin-agnostic `MainEntry` and a
Breeze `MainScreenBreeze`, and removes `UiMode` entirely. No upstream feature is removed.

## Why

Three problems, one shape.

**The root knew too much.** `ui/nav/BreezeMain.kt` held the theme, the settings read, the
navigator, the pager *and* the route table, so `ui/nav/` imported `ui/screen/`, `ui/viewmodel/`
and `ui/webui/`. A route key is the contract screens are plugged into; making it the thing that
mounts the screens inverts the dependency and makes the package untestable and unreusable.

**The skin had no seam.** Theme, blur, overlay host and navigation lived in different files, so
"replace the skin" meant editing five. `ui/BreezeShell.kt` is now that seam and nothing else: it
provides blur, the theme and the shared overlay window, and everything above it (navigator, pager,
every `NavEntry` view model store) survives a skin swap because `BreezeApp` wraps the host in
`movableContentOf` above the shell.

**`UiMode` was dead weight.** It had one constant, no writer, and `when (UiMode)` sites that could
never take another branch. The seam it described now exists for real, one composable wide.

The tab host split is what makes the second point testable: `MainEntry` resolves tab data and page
bodies with zero `ui.component.*` and zero `androidx.compose.material3.*` imports, and
`MainScreenBreeze` draws the bar and the pager with zero ViewModel imports. The compiler enforces
the direction, not a comment.

## Surface

```
kernel/           — none
userspace/ksud/   — none
manager/          — added: ui/BreezeApp.kt, ui/BreezeShell.kt,
                    ui/screen/main/MainEntry.kt, ui/screen/main/MainScreenBreeze.kt
                    rewritten: ui/MainActivity.kt (now ~60 lines: splash, edge-to-edge, one
                    setContent), ui/nav/{BreezeRoute,NavExt,BreezeIntentRouter}.kt,
                    ui/webui/{BreezeWebUIRoot,WebUIActivity,WebUIScreen,WebUIBreeze,
                    MonetColorsProvider}.kt
                    removed: ui/UiMode.kt, ui/nav/BreezeMain.kt,
                    ui/screen/MainScreenBreeze.kt, ui/viewmodel/MainActivityViewModel.kt,
                    ui/viewmodel/MainActivityUiState.kt
uapi/             — none
build / CI        — none (manager/build.gradle.kts minor-SDK line recorded as seam 3a)
```

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no kernel-side change |
| 2 Feature ids | not applicable | no kernel-side change |
| 3 Strings | not applicable | every label comes from an existing key (`home`, `superuser`, `module`, `settings`); none added or removed |
| 4 Manifest / gradle | not applicable | `WebUIActivity` keeps its declaration; `install()` and the splash stay in `MainActivity.onCreate` |
| 5 Breeze pages | **reshaped, none removed** | each screen keeps its `*Breeze.kt`/`*Entry.kt` pair. `MainScreenBreeze` moved to `ui/screen/main/` and lost its ViewModel and badge logic to `MainEntry`; `SuperUserPagerBreeze`/`ModulePagerBreeze` gained an `isActiveTab` parameter so leaving a search tab collapses and clears the field |
| 6 supercall wrappers | not applicable | untouched |
| 7 ksud CLI | not applicable | untouched |
| 8 Module parameters | not applicable | untouched |
| 9 Build workflows | not applicable | no asset or CI step |
| 10 Seams | **updated** | [seams.md](../seams.md) row 3a (`manager/build.gradle.kts`), row 4 (now `BreezeApp`), row 8 (`BreezeWebUIRoot` on `BreezeShell`), Part 2 `ui/` row (`UiMode` dropped) |

## Verified

```
grep -rn "UiMode|LocalUiMode|MainActivityViewModel|MainPagerConfig|LocalBreezeMainPagerState"
  manager/app/src  → no matches

grep -rn "^import me.weishu.kernelsu.ui.(screen|webui|viewmodel|BreezeApp|BreezeShell|MainActivity)"
  manager/.../ui/nav manager/.../ui/animation  → no matches

grep -n "ui.component" manager/.../ui/screen/main/MainEntry.kt       → no matches
grep -n "androidx.compose.material3" .../MainEntry.kt                → no matches
grep -n "viewmodel|ViewModel" manager/.../screen/main/MainScreenBreeze.kt → no matches

android_studio.build_project  → isSuccess: true (0 errors)
android_studio.lint_files     → 0 problems on every new/modified file
the spec gates                       → 9 gates pass
```

The `!isFullFeatured` degradation is preserved: `MainScreenBreeze` starts the navigation state at
`HIDDEN` and keeps it there when the kernel UAPI version does not match the manager's, and the
pager is not swipeable in that state.

## Sync risk

`MainActivity`, `WebUIActivity`, `SettingsRepository(Impl)` and `KernelSUApplication` exist
upstream with a different shape. An upstream edit to any of them is a signal, not a patch: read it
for the behaviour, then re-express it against `BreezeApp`/`BreezeShell` and `UiSettings`. A
change to upstream's `ui/navigation3/` never reaches the fork, because `BreezeRoute` and
`BreezeNavigator` are local copies.

`SuFilePathHandler` still reads the `ui_mode` preference and still expects `"breeze"`. If upstream
renames or drops that key, keep the fork's default rather than deleting the read: an installed app
written by an older build carries the value forward.

## Rollback

F4 is part of the one Manager rewrite commit, so `git revert` undoes the whole architecture.
A targeted rollback reverses that commit's composition-root slice:

```
git show "$(git log --grep='^manager: rebuild the Manager' -1 --format=%H)" -- 'manager/app/src/main/java/me/weishu/kernelsu/ui/{BreezeApp,BreezeShell}.kt' \
                          'manager/app/src/main/java/me/weishu/kernelsu/ui/nav/**' | git apply -R
```

No persisted state changes: the preference file keeps its keys, and
`UiMode`'s `ui_mode` string is still read only by `SuFilePathHandler`, which never depended on the
enum. A revert restores `UiMode`, `BreezeMain` and the two Activity ViewModel files intact.
