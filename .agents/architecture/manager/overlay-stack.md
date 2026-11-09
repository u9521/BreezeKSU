# Overlay layers and their animation

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/overlay/

The window and the two backdrop seams are in [overlay](overlay.md).

## One window, stacked layers

`BreezeOverlayHost` owns the single overlay window; each `BreezeOverlay` registers a `BreezeOverlayEntry` in `BreezeOverlayHostState.entries`.

- **Stack order is opening order (`entry.sequence`), not composition order**, so the newest modal draws on top wherever its call site sits.
- **Every layer paints its own backdrop; the stack is flat.** One keyed `forEachIndexed` loop, never recursion per index: nesting shifts a layer's position whenever the stack resizes, discarding its state and replaying its blur. A covered layer stays blurred and untouched.
- **Only layers below a sampling one are sampled**, via `hazeSource(zIndex = 2f + index)` above the page (`0f`) and search bar (`1f`), up to the last whose backdrop declares `samplesHost`.

## Modals stay composed until exit finishes

Both components animate their own exit and hold an internal `present` so the layer stays mounted (`visible = true`, `progress = OverlayProgress.Fade(visible)`) until it settles. Callers keep the composable mounted while closing, as `ConfirmDialogBreeze` does.

Sheets seeding `remember` from parameters (`MultiSelectBottomSheet`) keep `if (show)` and pass `visible = true`, trading the exit slide for fresh state; stateless ones pass `visible = show`.

## Backdrop vs. card animation

`BreezeOverlay` drives scrim alpha and blur radius from the one 0..1 value it is handed. `Fade` starts its `Animatable` at `0f` rather than using `animateFloatAsState`, so the initial enter runs instead of snapping to `1f`. Within a blurred stack `OverlayLayer` resolves `scope.scrimAlpha` once — `scrimAlphaBlurred` for the base, plus `scrimAlphaStacked` (`0.12f`) per layer above — so a stack reads as pushed back, not brighter.

`BreezeAlertDialog` slides 20dp and fades on the millis and easing its backdrop uses, so the two cannot drift; `BreezeOverlaySheet` delegates to M3's `BottomSheet`.

## Bottom sheets (`BreezeOverlaySheet`)

`BreezeOverlaySheet` hosts M3's `BottomSheet` inside `BreezeOverlay`, `TopCenter`-aligned so its anchors come from the full-screen overlay's constraints:

- **Height is content, never a parameter.** Give the body a scrollable `weight(1f)` child; below half a screen the partial and expanded anchors coincide.
- **The parameter shape mirrors `ModalBottomSheet`** (`sheetState`, `sheetMaxWidth`, `sheetGesturesEnabled`, `tonalElevation = 0.dp`) so a body moves between them unchanged. No `scrimColor` (it bypasses the `scrimAlphaBlurred = 0` rule) and no `properties` (its dimming conflicts with `OverlayLayer`'s back-key order). `backdrop` is the one addition, `null` by default so the sheet follows the host.
- **Caller-owned `SheetState`**, built with `rememberBottomSheetState`; the sheet never builds a second one.
- **Deferred `show()`:** waits for `hasExpandedState && sheetHeight > 0` plus a frame, so `BottomSheet` cannot promote the partial anchor at height 0.
- **Layer-owned back handling:** `backHandlerEnabled = false`; `OverlayLayer` registers `BackHandler` only for `isTopmost`.

### The backdrop follows the finger

`SheetFraction` turns `SheetState.requireOffset()` into the 0..1 the backdrop reads, so blur and scrim move with the drag itself; `show()`, `hide()` and the anchor settling move that same offset. Reach is the sheet height, and the travel runs through `OverlayEnterEasing` at the progress exit — one curve that needs no partial-anchor knowledge.

**Dismissal shares the timeline.** A drag leaves `visible` true and only sets `dismissing`, so the fade follows `visible && shown && !dismissing`, and the layer unmounts only once `hide()` **and** the `OverlayExitMillis` fade finish; unmounting when `hide()` returns pops the blur.

### The sheet consumes its own drag offset

`consumeWindowInsets(SheetWindowInsets(sheetState))` reads the sheet's top offset — 0 until anchors exist — and consumes exactly the status-bar strip the sheet covers: a partial sheet shows no gap, an expanded one paints under the bar. `contentWindowInsets` (default `modalWindowInsets`) pads handle and content together, never horizontal padding.

Two rules keep it safe: `equals` compares **values**, since one that never holds means "changed every frame", and only the top edge is read. It replaced Material's `OffsetInsets`, whose reference equality trembled the sheet at ~60 Hz.
