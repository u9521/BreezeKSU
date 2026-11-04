# Manager modal overlays

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/overlay/, manager/app/src/main/java/me/weishu/kernelsu/ui/util/HazeExt.kt

How layers stack and how their cards and scrims animate is in [overlay-stack](overlay-stack.md).

## A modal overlay owns a full-screen window

`BreezeOverlay` is the only window modals are built in, and six invariants depend on it staying full-screen:

- **Its content root must be `fillMaxSize()`**, so `DialogLayout.internalOnMeasure` escalates `WRAP_CONTENT` to `MATCH_PARENT` on frame one; otherwise a height change is a `WindowManager` relayout.
- **`dismissOnClickOutside` must stay off**: every touch over a full-screen window is inside the child's rect, so the platform test never fires. Outside taps go through the Compose scrim, and `scrimAlpha = 0f` disables platform dimming.
- **Never animate height from the window** — fixed-window changes are free, content-sized ones cost an IPC per frame.
- **Clear the window's own animations.** `OverlayWindow` calls `setWindowAnimations(0)`, or the theme scales the backdrop.
- **Keep the scrim composed for the layer's whole life.** It carries the outside-tap handler and accessibility barrier; skipping it at alpha 0 lets touches fall through mid-transition.
- **Dimming is the fallback for a backdrop that samples nothing.** One with `samplesHost = true` in a recording frame uses `scrimAlphaBlurred` (`0f`); otherwise `scrimAlpha` (`0.6` dialogs, `0.32` sheets).

Attaching the `Dialog` costs ~400 ms on the AVD while the page stays live; an empty pre-created window is not viable.

## Where progress comes from (`OverlayProgress`)

Blur radius, scrim opacity and the stacked dim all read one 0..1 value. `Fade(visible, …)` animates a private `Animatable` and is the default; `External { value() }` forwards a caller value once per frame, which is how `BreezeOverlaySheet` ties the backdrop to its drag. An `External` source must be safe before it has a position and stay inside `0f..1f`. The easing pair and 220/150 ms live in `OverlayProgress.kt` and are what `BreezeAlertDialog` animates its card on, so card and backdrop share one timeline.

## What is painted behind the card (`OverlayBackdrop`)

One implementation slot, not a set of flags: `Backdrop(scope, content)` receives progress, `hazeState`, the resolved scrim alpha, `isTopmost` and the outside-tap callback. `Default` is host blur with a dialog's scrim, `sheetBlur()` the same with a sheet's, `Scrim` an opaque scrim that records nothing.

**`samplesHost` is both the declaration and the switch**, so a backdrop that never samples the host records nothing. `OverlayLayer` resolves the stacked dim into `scope.scrimAlpha`, so a backdrop honours it instead of recomputing it.

**Both components default `backdrop` to `null`, following the host.** A non-null default outranks it: "blur off" would mean "dialog sharp, sheet blurred".

## Backdrop blur is self-contained

`BreezeOverlayHost` registers the page below with `hazeSource(state.hazeState, zIndex = 0f)` **only while some visible layer's backdrop samples it**, so a closed overlay or a stack of sharp layers costs no offscreen capture. It also paints `background(colorScheme.background)`, or unpainted regions stay sharp.

**The host decides blur, not `LocalEnableBlur`.** Overlay components never read that setting: `BreezeOverlayHost(backdrop = …)` is the stack-wide default (`BreezeShell` maps `settings.enableBlur` onto `Default` or `Scrim`) and a layer's own `backdrop` overrides it; `LocalEnableBlur` gates only bars and buttons. Each entry's `backdrop` is snapshot-backed, so switching a live layer invalidates the capture during composition, not a frame late.

A covering surface above the page but below the overlay window, such as the expanded `SearchAppBarBreeze`, calls `Modifier.overlayBackdrop(zIndex = 1f)`: a no-op while nothing is captured, never a permanently opaque background. An overlay opened from the expanded search bar then blurs the search bar, not the page.

## Haze 2.x

Blur goes through `hazeBlur(input = HazeInput.Sources(state), style = …)`; the 1.x `hazeEffect`/`hazeChild` pair is gone. `HazeBlurStyle { }` takes a **non-`@Composable`** lambda, so read theme colours before entering it (`topBarHazeStyle` does). `OverlayBlur` carries Haze's input scale and noise beside the radius so all overlays share one grain.
