---
status: modify
mechanism: surgery
residue-rev: 1
---

# P1 — WebUI host rebuilt as a state machine

Ports the sibling fork's `wuiref` branch (`43e1cc76`, `8b719839`, `8d26101b`) onto Breeze's
layering. The WebUI host stops being one mutable class owning the WebView, the root shell, the
pending `ValueCallback`, the dialog and the load state, and becomes `WebUIViewModel` (state +
effects) + `WebUIRuntime` (activity-scoped handles) + a `webview/` package.

## Why

The old `WebUIState` was a plain class with a dozen public `var`s, including the `WebView`
itself. Three of its fields were the same field: `uiEvent` was both the load state and the active
JavaScript dialog, so answering a dialog wrote `WebViewReady` and cleared "loading" as a side
effect. The file chooser stored its callback with no request identity, so a late answer could be
handed to a newer request.

`wuiref` splits this correctly, but it is written against upstream's `UiMode` enum and
`ui/viewmodel/SuperUserViewModel`, neither of which exists here. The port keeps the *shape* and
rewrites the couplings.

## Surface

```
manager/app/src/main/java/me/weishu/kernelsu/ui/webui/
  model/          WebUILoadState, WebUIState, WebUIEffect, WebUIDialog, WebUIBanner, ModuleInfo
  viewmodel/      WebUIViewModel          — state + effect Channel, no Context, no Compose
  webview/        WebViewPreparer, WebViewClients, WebViewConstants,
                  asset/SuFilePathHandler — Kotlin, serves insets.css from env()
  ui/             WebUIScreen, WebUIOverlayHost, WebUILoadingMask, WebUIEventHandler,
                  component/{WebViewContainer,OutdatedWebViewBanner,dialog/WebUIDialogContent}
  WebUIRuntime    — WebView + open shells + pending chooser, scoped by remember {}
  WebUIEntry      — the *Entry of the host: viewModel(), runtime, effect collector
  WebUIActivity   — 30 lines: the window and nothing else
manager/…/ui/util/ModuleIcon.kt   — loadModuleIconBitmap, extracted from Shortcut
manager/…/ui/util/WebUIIntent.kt  — createWebuiIntent, resolves the activity by component name
```

Deleted: `WebUIState.kt`, `WebUIScreen.kt` (root), `WebViewHelper.kt`, `WebUIBreeze.kt`,
`SuFilePathHandler.java`, `MimeUtil.java` (moved to `ui/webui/util/`), `Insets.kt`. `Insets.kt` is
not ported because `env(safe-area-inset-*)` makes pushing pixel values from Kotlin unnecessary —
and in `wuiref`'s use of it, wrong.

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1, 2, 7–9 | not applicable | no supercall, no `KSU_FEATURE_*` id, no CLI surface, no bootconfig key, no embedded asset or CI step. |
| 3 Strings | added | five: `webview_outdated_{title,message}` and `webui_{failed_to_open_link,invalid_module_id,render_process_crashed}`, in `values` and `values-zh-rCN`. `webview_outdated_{update,dismiss}` were added and then removed: the card is the update action and the close button is the dismiss action, so neither has a caller. |
| 4 Manifest / gradle | edited | `MainActivity` gains `launchMode="singleTask"`; `WebUIActivity` and its `taskAffinity`/`documentLaunchMode` are unchanged. No dependency added: `lifecycle-viewmodel-compose` was already in use by six `*Entry.kt` files. |
| 5 Breeze pages | new funnel | `ui/webui/` keeps the three-file shape (`viewmodel/` + `WebUIEntry` + `WebUIScreen`); `verify-arch-layers` passes at 307 files. |
| 6 supercall wrappers | unchanged | `WebViewInterface` still reaches `ksu.pm()`, `ksu.io()`, `ksu.exec()` through the same wrappers. |
| 10 Seams | edited | `webUIApps()` moved to `WebUIApps.kt` (it lived in the deleted `WebViewHelper.kt`); `loadShortcutBitmap` moved from `ui/screen/module/Shortcut.kt` to `ui/util/ModuleIcon.kt`; two launch sites now call `createWebuiIntent`. |

## The IME question, measured

The port first treated the IME as a host-padding problem. It is not, and the measurement is what
settled it. On the AVD (`emulator-5554`, API 37, WebView `149.0.7827.5`), host padding at zero:

```
keyboard down   view 2400px   innerHeight 914   visualViewport.height 914
keyboard up     view 2400px   innerHeight 914   visualViewport.height 578
```

The view and the layout viewport do not move; only the visual viewport shrinks. Two earlier
readings were wrong because they measured a padded view and blamed the system:
`WindowInsets.safeDrawing` already is `systemBars ∪ ime ∪ displayCutout` (verified in
`WindowInsetsHolder`'s bytecode), so a "bars only" padding of 2263px that fell to 1443px on focus
was the host's own padding. Writing the IME union out again changes nothing and reads as though it
were missing.

So: **edge-to-edge on** — the page pads itself from `internal/insets.css`, resolved by WebView
(measured 29px top / 24px bottom). **Edge-to-edge off** — the host pads `WindowInsets.safeDrawing`;
view 2263 -> 1443 on focus, `innerHeight` 862 -> 549. The two do not compound.

`MIN_INSETS_VERSION = 144` survives for the safe-area CSS only, and it is load-bearing: the host
stops padding only when `isEdgeToEdge && isInsetsSupported`. Below 144 `env()` is `0px`, so a host
that also stopped padding would leave the content under the status bar and the cutout. That branch
is unreachable on a WebView 149 device and no override hook was kept for it.

## Where this port differs from `wuiref` on purpose

The port aims to match `wuiref` except where that behaviour is a defect. The first list is what
was not carried over; the second survives on purpose, so a future reader does not "fix" it back.

Defects, not ported:

| `wuiref` | What it does wrong |
| :--- | :--- |
| `HandleInsets` | Converts insets to **dp** (`getTop(density) / density.density`) and writes them as `px`: on a 3.0-density device an 87px status bar is published as 29px. It sets them as inline styles, which outrank the `env()` declarations, so the wrong number wins. |
| the layout gate | Pads on `isEdgeToEdge` alone. Below WebView 144 `env()` is `0px`, so neither side pads and the content runs under the status bar and the cutout. |
| `KsuIO`, `BlobDownloadHandler` | Both process-wide `object`s, so closing one module's window closes another's open streams and empties its pending downloads. |
| `onRenderProcessGone` | Returns `true` — promising the view was already dropped — but keeps it. Touching a dead WebView afterwards kills the process. |
| the file chooser | An effect that finds no matching request id does nothing, leaving the page blocked on a picker that will never answer. |
| `emitEffect` / collector | `launch { send() }` lets two effects arrive out of order; a bare `LaunchedEffect` lets `Finish` run while the window is stopped. |

Differences kept:

| Area | This port | `wuiref` | Why |
| :--- | :--- | :--- | :--- |
| dialog title | module name | none for alert/confirm; the message doubles as the prompt's label | Breeze's `ConfirmDialogBreeze` has a title slot; an empty one is worse than the module name. |
| system bars | `enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT))` | bare `enableEdgeToEdge()` | The bare form picks icon contrast from the **system** setting; Breeze's comes from `settings.colorMode`. Same call as `MainActivity`. |
| theme changes | settings flow recomposes `BreezeWebUIRoot` | preference listener + `recreate()` | `recreate()` tears down the WebView and reloads the page for a colour change. |
| `colors.css` | always served | only when `colorMode in 3..6` or `ui_mode == "material"` | `ui_mode` has no writer here (F1 removed the setting), so the condition could only ever be true. |
| module validation | re-read on every create | `isModuleValidated` skips the scan | The cache is bundled with `recreateWebView()`, which also resets the dialog, the back state and the load state. Not worth the coupling for a rare path. |
| `adjustNothing` | not set | set on `WebUIActivity` | Measured without it: the view stays 2400px with the keyboard up. If a page is ever found to move with the keyboard, add the flag back first. |

One difference was **reverted**: `SwipeToDismissBox` was dropped during the port, on the argument
that a banner which cannot be brought back should not take a gesture. That did not justify
overriding the reference — swipe and close button call the same `onDismiss`, so the gesture is a
second route to an existing action. It is back.

## Verified

```
gradlew :app:assembleDebug                    -> BUILD SUCCESSFUL
the ten spec gates                            -> all pass
```

On the AVD with `adb root`:

```
ksuwebui_demo opens, renders, catalog scrolls
dialog demo -> window.alert() reaches a Breeze AlertDialog, Confirm answers the page
two modules       -> two tasks (t85, t86); reopening one brings its task forward, does not stack
MainActivity      -> "its current task has been brought to the front" (singleTask works)
insets.css        -> --safe-area-inset-top: 29px, --safe-area-inset-bottom: 24px
```

## Sync risk

The three `wuiref` commits are not on upstream's branch list, so a future sync will not deliver
them; this fork owns the code now. Upstream can still rewrite `WebViewHelper.kt` — a file that no
longer exists here, which makes the deletion visible rather than silent. The `UiMode`-shaped
halves of `wuiref` (Material/Miuix dialog pairs, `OutdatedWebViewBanner*` duplicates,
`WebUIActivity`'s preference listener) were deliberately not ported; re-adding one would
reintroduce a second renderer.

The two lists above are the sync risk in the other direction: `wuiref` still carries all eight
defects, so anything ported back has to fix them there rather than copy them here.

## Rollback

Revert both commits. `WebUIState.kt`, `WebViewHelper.kt` and the Java `SuFilePathHandler` come
back as they were; no persisted data, ABI number or feature id changed.
