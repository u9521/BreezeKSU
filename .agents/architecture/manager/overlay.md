# Manager modal overlays

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/overlay/, manager/app/src/main/java/me/weishu/kernelsu/ui/util/HazeExt.kt

How layers stack, and how their cards and scrims animate, is in [overlay-stack](overlay-stack.md).

## A modal overlay owns a full-screen window

`BreezeOverlay` (`ui/component/breeze/overlay/`) is the only window modals are built in, and six invariants depend on it staying full-screen:

- **Its content root must be `fillMaxSize()`.** That makes `DialogLayout.internalOnMeasure` escalate `WRAP_CONTENT` to `MATCH_PARENT` on frame one. Without it the window is content-sized, turning every height change into a `WindowManager` relayout and stutter.
- **`dismissOnClickOutside` must stay off.** Over a full-screen window every touch is inside the child's rect, so the platform test never fires. `BreezeOverlay` handles outside taps on its Compose scrim and sets `scrimAlpha = 0f` to disable platform dimming.
- **Never animate height from the window.** Fixed-window height changes are free; content-sized ones cost an IPC per frame.
- **Clear the window's own animations.** `OverlayWindow` calls `setWindowAnimations(0)` so the theme's window scale animation does not scale the full-screen backdrop.
- **Keep the scrim composed for the layer's whole life.** `Scrim` carries the outside-tap handler and accessibility barrier; skipping it while alpha is 0 lets touches fall through to the page during enter/exit transitions.
- **Dimming is the fallback for a layer that paints no blur.** A layer resolving to a blur uses `scrimAlphaBlurred` (`0f`, the 20dp blur already separates the card); one resolving to none falls back to `scrimAlpha` (`0.6` dialogs, `0.32` sheets).

## Window creation latency (~400 ms)

Attaching a platform `Dialog` window takes ~400 ms on the AVD (~40 ms in Compose, the rest in `WindowManager`). Until the window attaches, the page underneath remains live; pre-creating an empty window is not viable, so this gap is accepted.

## Backdrop appearance (`OverlayAppearance`)

`OverlayAppearance` bundles `blur`, `blurRadius`, `scrimAlpha`, `scrimAlphaBlurred`, and shared `enterMillis` / `exitMillis` so blur and scrim share one timeline. `OverlayAppearance.Default` is for dialogs and `OverlayAppearance.Sheet` for sheets; callers tweak fields via `copy()`. `OverlayBlur` encapsulates Haze's input scale and noise alongside the per-frame radius so all overlays share one grain.

## Backdrop blur is self-contained

`BreezeOverlayHostState` owns its `hazeState`, and `BreezeOverlayHost` registers the page below with `hazeSource(state.hazeState, zIndex = 0f)` **only while some visible layer would actually blur** — a closed overlay or a stack of sharp layers costs no offscreen capture, and no caller wires a source by hand.

**The host decides blur, not `LocalEnableBlur`.** Overlay components never read that setting: `BreezeOverlayHost(backdropBlur = …)` is the stack-wide default (`BreezeShell` maps `settings.enableBlur` onto it) and the nullable `OverlayAppearance.blur` overrides it per layer; `LocalEnableBlur` gates only bars and buttons. Each layer's `appearance` is snapshot-backed on `BreezeOverlayEntry`, so switching a live layer's blur invalidates the capture during composition, not a frame late.

That registration also paints `background(colorScheme.background)`: unpainted regions would otherwise stay transparent and show sharp content through the blur.

A covering surface — one drawn above the page but below the overlay window, such as the expanded `SearchAppBarBreeze` — calls `Modifier.overlayBackdrop(zIndex = 1f)` (`ui/component/breeze/overlay/`). It registers into the same host state one step above the page and is a no-op returning the receiver while the host captures nothing (no overlay showing, or no visible layer blurs); it must never be replaced by a permanently opaque background. An overlay opened from the expanded search bar then blurs the search bar, not the page behind it.

## Haze 2.x

Blur goes through `hazeBlur(input = HazeInput.Sources(state), style = …)`; the 1.x `hazeEffect`/`hazeChild` pair is gone. `HazeBlurStyle { }` takes a **non-`@Composable`** lambda: read theme colours before entering it (`topBarHazeStyle` does), and you cannot call `MaterialTheme` inside. `LocalEnableBlur` gates bars and buttons at the call site; overlay blur is gated by the host's `backdropBlur` instead, so Haze never runs with the setting off either way.
