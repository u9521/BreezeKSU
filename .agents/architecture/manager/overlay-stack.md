# Overlay layers and their animation

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/overlay/

The window those layers are drawn in, and the blur they share, are in [overlay](overlay.md).

## One window, stacked layers

`BreezeOverlayHost` owns the single overlay window; each `BreezeOverlay` registers a `BreezeOverlayEntry` in `BreezeOverlayHostState.entries`.

- **Stack order is opening order (`entry.sequence`), not composition order**, so the most recently opened modal always draws on top regardless of where its call site sits in the tree.
- **Only the topmost layer blurs.** Each lower layer registers into the shared `HazeState` via `hazeSource(zIndex = 2f + index)` — above the page (`0f`) and expanded search bar (`1f`). The topmost layer's single effect blurs the page and all lower layers in one pass.

## Modals stay composed until exit finishes

Both `BreezeAlertDialog` and `BreezeOverlaySheet` animate their own exit and hold an internal `present` state so the layer stays mounted (`visible = true`, `scrimVisible = visible`) until the card transition settles. Callers wanting an exit animation keep the composable in the tree while closing (`Dialog(visible = shown)`, not `if (shown) Dialog()`) and retain the body until exit finishes, as `ConfirmDialogBreeze` does with `lastBody`.

Sheets seeding internal `remember` state from parameters (`MultiSelectBottomSheet`, `SeLinuxEditBottomSheet`) keep `if (show)` and pass `visible = true`, trading the exit slide for fresh state; stateless or hoisted ones (`SendLogBottomSheetBreeze`, `ModuleShortcutSheet`) pass `visible = show` and animate out.

## Backdrop vs. card animation

`BreezeOverlay` drives both scrim alpha and blur radius from a single `Animatable(0f)` (`enterMillis = 220`, `exitMillis = 150`). Starting from `Animatable(0f)` rather than `animateFloatAsState` ensures the initial enter transition runs instead of snapping to `1f`. When `blurActive` (`isTopmost && LocalEnableBlur.current && hazeState != null`) holds, the scrim uses `scrimAlphaBlurred` (`0f`); otherwise it falls back to `scrimAlpha`.

Each component animates its own card: `BreezeAlertDialog` slides 20dp vertically and fades (220 ms / 150 ms, matching `Animation.Material.Dialog`), while `BreezeOverlaySheet` delegates to Material3's `BottomSheet`.

## Bottom sheets (`BreezeOverlaySheet`)

`BreezeOverlaySheet` hosts Material3's `BottomSheet` inside `BreezeOverlay`, aligned `TopCenter` so its anchors come from the full-screen overlay's constraints:

- **Height is content, never a parameter.** Give the body a scrollable `weight(1f)` child to fill the window; a sheet shorter than half the screen has its partial and expanded anchors coincide.
- **The parameter shape mirrors `ModalBottomSheet`** (`sheetState`, `sheetMaxWidth`, `sheetGesturesEnabled`, `tonalElevation = 0.dp`) so a body moves between the two unchanged. No `scrimColor`: it would bypass the `scrimAlphaBlurred = 0` anti-flicker rule above. No `properties`: its platform dimming conflicts with `OverlayLayer`'s back-key order.
- **Caller-owned `SheetState`:** build it with `rememberBottomSheetState` and pass it; the sheet never constructs a second one.
- **Deferred `show()`:** Waits for `hasExpandedState && sheetHeight > 0` plus one frame, preventing `BottomSheet` from promoting the partial anchor to expanded while anchors are converged at height 0.
- **Layer-owned back handling:** `backHandlerEnabled = false`; `OverlayLayer` registers `BackHandler` only for `isTopmost`.

### The sheet consumes its own drag offset

The sheet passes `consumeWindowInsets(SheetWindowInsets(sheetState))` on the `BottomSheet`. That inset reads the sheet's current top offset — 0 until anchors exist — and consumes exactly the status-bar strip the sheet already covers, which is why a partially expanded sheet shows no status-bar-high gap above it while an expanded one paints under the status bar. `contentWindowInsets` (default `modalWindowInsets`) then pads the handle and content together inside the surface; horizontal padding does not belong in it.

Two rules make the value safe: `equals` compares **values** (`other.state == state`, hash on the state) — a `WindowInsets` whose `equals` never holds means "changed every frame" to the layout system — and only the top edge is read, with left/right/bottom at 0. It replaced Material's internal `OffsetInsets`, whose reference equality fed a measure-pass loop that trembled the sheet at ~60 Hz while the finger was still.
