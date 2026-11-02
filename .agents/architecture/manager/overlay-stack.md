# Overlay layers and their animation

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/overlay/

The window those layers share is in [overlay](overlay.md).

## One window, stacked layers

`BreezeOverlayHost` owns the single overlay window; each `BreezeOverlay` registers a `BreezeOverlayEntry` in `BreezeOverlayHostState.entries`.

- **Stack order is opening order (`entry.sequence`), not composition order**, so the newest modal draws on top wherever its call site sits.
- **Every layer paints its own blur; the stack is flat.** Layers render in one keyed `forEachIndexed` loop, never recursion per index: nesting shifts a layer's position whenever the stack size changes, discarding its state and replaying its blur from zero. A covered layer stays blurred and returns untouched when the one above it leaves.
- **Only layers below a blurring one are sampled.** Each registers via `hazeSource(zIndex = 2f + index)` — above the page (`0f`) and search bar (`1f`) — up to the last layer that blurs; nothing above reads them.

## Modals stay composed until exit finishes

Both `BreezeAlertDialog` and `BreezeOverlaySheet` animate their own exit and hold an internal `present` state so the layer stays mounted (`visible = true`, `scrimVisible = visible`) until the transition settles. Callers wanting an exit animation keep the composable mounted while closing and retain the body until exit finishes, as `ConfirmDialogBreeze` does with `lastBody`.

Sheets seeding internal `remember` state from parameters (`MultiSelectBottomSheet`, `SeLinuxEditBottomSheet`) keep `if (show)` and pass `visible = true`, trading the exit slide for fresh state; stateless or hoisted ones pass `visible = show` and animate out.

## Backdrop vs. card animation

`BreezeOverlay` drives scrim alpha and blur radius from one `Animatable(0f)` (`enterMillis = 220`, `exitMillis = 150`). Starting from `0f` rather than `animateFloatAsState` makes the initial enter transition run instead of snapping to `1f`.

A layer's blur is `OverlayAppearance.blur` when set, else the host's `backdropBlur`. Resolving to none keeps the plain `scrimAlpha` — nothing else separates that card. Above the base, each layer adds `scrimAlphaStacked` (`0.12f`) so a stack reads as pushed back, not brighter: the base uses `scrimAlphaBlurred` (`0f`), layers above it `scrimAlphaBlurred + scrimAlphaStacked`.

Each component animates its own card: `BreezeAlertDialog` slides 20dp and fades (220 ms / 150 ms, matching `Animation.Material.Dialog`); `BreezeOverlaySheet` delegates to Material3's `BottomSheet`.

## Bottom sheets (`BreezeOverlaySheet`)

`BreezeOverlaySheet` hosts Material3's `BottomSheet` inside `BreezeOverlay`, aligned `TopCenter` so its anchors come from the full-screen overlay's constraints:

- **Height is content, never a parameter.** Give the body a scrollable `weight(1f)` child; below half a screen, partial and expanded anchors coincide.
- **The parameter shape mirrors `ModalBottomSheet`** (`sheetState`, `sheetMaxWidth`, `sheetGesturesEnabled`, `tonalElevation = 0.dp`) so a body moves between the two unchanged. No `scrimColor` (it would bypass the `scrimAlphaBlurred = 0` rule above) and no `properties` (its dimming conflicts with `OverlayLayer`'s back-key order). `appearance` is the one addition; it defaults to `OverlayAppearance.Sheet`.
- **Caller-owned `SheetState`:** build it with `rememberBottomSheetState` and pass it; the sheet never builds a second one.
- **Deferred `show()`:** waits for `hasExpandedState && sheetHeight > 0` plus one frame, so `BottomSheet` cannot promote the partial anchor while anchors are converged at height 0.
- **Layer-owned back handling:** `backHandlerEnabled = false`; `OverlayLayer` registers `BackHandler` only for `isTopmost`.

### The sheet consumes its own drag offset

The sheet passes `consumeWindowInsets(SheetWindowInsets(sheetState))` on the `BottomSheet`. That inset reads the sheet's current top offset — 0 until anchors exist — and consumes exactly the status-bar strip the sheet covers, so a partial sheet shows no status-bar-high gap while an expanded one paints under the bar. `contentWindowInsets` (default `modalWindowInsets`) then pads handle and content together; horizontal padding does not belong in it.

Two rules keep it safe: `equals` compares **values** (`other.state == state`, hash on the state) — an `equals` that never holds means "changed every frame" — and only the top edge is read. It replaced Material's `OffsetInsets`, whose reference equality fed a measure-pass loop that trembled the sheet at ~60 Hz.
