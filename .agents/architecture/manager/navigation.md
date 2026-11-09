# Manager navigation and split scenes

Covers: manager/app/src/main/java/me/weishu/kernelsu/ui/nav/, manager/app/src/main/java/me/weishu/kernelsu/ui/animation/, manager/app/src/main/java/me/weishu/kernelsu/ui/util/SplitRatioSource.kt, manager/app/src/main/java/me/weishu/kernelsu/ui/util/SplitPaneLocals.kt, manager/app/src/main/java/me/weishu/kernelsu/ui/util/PaneSlot.kt, manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/button/PaneBackButton.kt, manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/button/SplitScreenRatioButton.kt

## The back stack is the layout

`ui/nav/` is layer 4: it imports layers 1–3 and never `ui/screen`, `ui/webui` or the composition root. `BreezeIntentRouter` therefore reports a zip via `onRequestInstallModule` and a WebUI link via `onOpenWebUI`; the root owns the dialog and the activity. A new tab edits `BreezeRoute.TabRoute` alone — order, label and icon derive from the enum.

`push` appends, including a key whose class already sits on top: two app profiles are two pages. `pushOrReplaceSameClass` is for a detail a page presents as "the open item", so the stack keeps its size and one back press still leaves. Pushing a `TabRoute` resets the stack to it.

## Panes pair by marker, not by type

`nav/PaneGroup.kt` carries two interfaces. `PaneHost` names the subject a route can talk about; `PaneDetail` lists the subjects it can appear beside. Adjacent entries pair when the older one's `paneGroup` is in the newer one's `paneGroups` and their `contentKey` differ. A route may be both — `AppProfile` is `PaneDetail(SuperUser)` and `PaneHost(App)`, which chains all four profile pages into one run.

The markers travel as `NavEntry` metadata (`NavKey.paneMarkers()`, filled in the root's `entryProvider`), because navigation3 keeps an entry's key private and a strategy cannot read the route back. A route that neither hosts nor splits publishes nothing, so the scene never swallows a page that could only show one.

## One strip, one cursor

`BreezeListDetailScene` does not draw two columns. It lays the trailing run of pairable entries along a horizontal strip and moves a camera cursor over it: page `i` sits at offset `i - cursor`, offset 0 is the start column and 1 the end, and only `[-1, 2]` can show a pixel. Pushing is `cursor + 1`; nothing leaves the composition, so drilling deeper never runs a scene transition.

Three `Animatable`s drive it: `cursor` (which page is in the start column), `seam` (how far the columns are open — the start column is full width until a second page exists) and `cover` (a replaced end-column page sliding in from beyond the right edge instead of pushing the old one aside). `PredictiveBackHandler` snaps `cursor` or `seam` to the finger and calls `onBack()` before release, so the shorter stack and the released finger land in the same frame.

Two invariants hold. Each `contentKey` composes **exactly once** — `SaveableStateHolder` throws on a duplicate, so the stack is deduped and the window filtered first. And `previousEntries` is always the whole stack minus its top, because nav3 derives the back step from its size.

## Position is the only thing a page may ask

`LocalPaneSlot` publishes `Start` or `End`; `null` means full screen. It flips once, when the cursor passes halfway across the page, so a page travelling between columns does not swap its chrome on the first frame. `PaneBackButton` renders only outside `End`, where the page is covered by the entry that pushed it. `LocalPaneHasNext` is the one fork addition: it tells a start-column page something is beside it, which `SuperUserBreeze` needs to decide whether its list offers a back arrow at all.

## The ratio belongs to the caller

A scene never owns the column ratio. `SplitRatioSource.ratioFor(startKey, endKey)` returns a `MutableFloatState`; `BreezeApp` provides one source above `NavDisplay`, and the scene reads it per pair **inside** `content`, never into its identity — otherwise `NavDisplay` rebuilds the scene instead of resizing the columns. One shared state means one global width; a state per pair means each pair keeps its own. `SplitScreenRatioButton` writes to `LocalSplitRatio` and renders only in `End`.
