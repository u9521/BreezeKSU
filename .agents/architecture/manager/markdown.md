# Markdown and dialogs

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/markdown/, manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/dialog/

## The renderer

Markdown is Compose too. `LazyMarkdownContent` owns its scroller and composes only blocks near the viewport — never wrap it in one. Its parse state is a parameter: parsing is async, and the update dialog observes it to disable its actions until the document exists.

`BreezeMarkdownStyle` is the bundles `m3.Markdown` takes; one preset `Default` (see [F2](../../../doc/entries/F2-compose-markdown.md)). Pass every overridden slot in one `markdownComponents(...)` call. Markers and checkboxes are drawn, not glyphs: the font is the reader's.

## The dialog

`ConfirmDialogBreeze` keeps no state the caller must sync: its body is a `ConfirmBody` owned and replaced via `setConfirm`, and the only internal state is the hoisted markdown parse subscription that keeps confirm disabled until parsing finishes.

Three rules keep it from growing back into a framework:

- **The wait is a parameter, not a wrapper.** `ConfirmBody.loading` and the hoisted parse state both swap the body for one spinner and disable confirm while either is active. (`LoadingDialogState.withLoading` is for blocking work with no dialog body; do not move it into `ConfirmBody`.)
- **What the body means is the caller's.** Call sites choose `Body.Text`, `Body.Markdown`, `Body.Empty` or `Body.Custom`. The dialog knows layout, actions and the wait; it never learns what a changelog is.
- **The card may animate its height, the window may not.** `ConfirmDialogBreeze` runs inside `BreezeAlertDialog`, i.e. inside the full-screen `BreezeOverlay` window, so its `animateContentSize` is an ordinary layout pass in a fixed-size window: zero `WindowManager` relayout, no growth stutter. This holds *only* while it stays there. Re-host it on a platform `AlertDialog`, which sizes its window to content, and every height change becomes an IPC per frame — the old reason for keeping the spinner and content as two overlaid layers and animating only their alphas. That layering is still what the cross-fade does, but the constraint now lives in the window, not the card.

Nothing per-dialog wires up blur: the backdrop is the overlay host's own `hazeState` (see [overlay](overlay.md)). There is no `Modifier.windowBlurBehind()` to lose, which is the point — a dialog cannot silently shed its blur by reshaping a body.

`Body.Empty` carries its icon and optional action as data, so the three empty-changelog outcomes (`LinkOff`, `Draft`, `CloudOff` with retry) stay distinct values — see `ConfirmBody.Body.Empty`.
