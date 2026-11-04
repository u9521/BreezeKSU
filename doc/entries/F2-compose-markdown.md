---
status: prune
mechanism: swap
residue-rev: 1
---

# F2 — markdown rendering: Compose instead of WebView

Replaces upstream's WebView markdown renderer with a Compose-native one, and gives the Manager
one reading style tuned for module changelogs on a phone.

## Why

Upstream hands the string to a WebView that loads a bundled HTML template, a stylesheet pair and
a JavaScript highlighter: one instance, one layout pass, and an asynchronous callback before
anything is on screen, paid by every changelog and every install confirmation.

The Manager shows markdown in module changelogs (`updateJson`) and install confirmations inside a
dialog. A WebView is the wrong instrument, and the `commonmark` dependency plus four extensions,
which pre-parsed markdown into HTML *for* that WebView, existed only to feed it. The Compose
renderer parses to an AST and composes blocks lazily, while the dialog holds its spinner until
parsing finishes.

## Surface

```
kernel/           — none
userspace/ksud/   — none
manager/          — removed: ui/component/markdown/GithubMarkdown.kt, ui/component/dialog/BreezeDialogs.kt,
                    and the 10 Markdown WebView files under src/main/assets/webview/ (download.js,
                    which serves Module WebUI, is not one of them and stays)
                  — added: ui/component/markdown/style/ (BreezeMarkdownStyle, BreezeMarkdownDefault,
                    BreezeMarkdownComponents), MarkdownBrokenImage.kt, 5 generated icons
                    (BrokenImageFilled, CloudOff, ContentCopyFilled, Draft, LinkOff), and
                    src/screenshotTest/.../preview/{MarkdownSamples,MarkdownStylePreview}.kt
                  — rewritten: MarkdownContent.kt, Dialog.kt, DialogBreeze.kt, DialogBreezePreview.kt;
                    updated callers in ModuleViewModel, ModuleUiState, ModuleBreeze, ModuleUpdateInfo,
                    ModuleRepositoryImpl, InstallModuleDialog, UninstallDialogBreeze, RebootListPopup,
                    SendLogBottomSheetBreeze, HomeBreeze, HomeEntry, InstallBreeze, InstallEntry
uapi/             — none
build / CI        — libs.versions.toml drops commonmark x5, adds
                    multiplatform-markdown-renderer + -m3
```

### Style and components

`BreezeMarkdownStyle` holds the exact bundles `com.mikepenz.markdown.m3.Markdown` renders from
(`colors`, `typography`, `padding`, `dimens`, `extendedSpans`, `components`), exposed as the
`@Composable` preset `BreezeMarkdownStyle.Default`.

Tuned against published module changelogs for a 312dp dialog slot on `surfaceContainerHigh`:
H1–H3 are equal-role `primary` release headings separated by section rules; lists hang from a
fixed marker column with canvas-drawn bullets and 16dp checkboxes (independent of the reader's
font); code blocks carry a copy header and `SelectionContainer`; wide top-level tables bleed
across `contentPadding` to the dialog edge with layout-determined square/rounded corners; column
alignments (`:---`, `:---:`, `---:`) are parsed from `TABLE_SEPARATOR`.

### Lazy rendering, no external resources, and value-driven dialogs

`LazyMarkdownContent(content, style, state, modifier, contentPadding, onLinkClick)` owns a
`LazyColumn` over visible top-level AST nodes (skipping `EOL` and `LINK_DEFINITION` gaps) so large
changelogs compose only blocks near the viewport. `rememberLazyMarkdownState` pins the parser
inputs across recompositions so the state does not reset to `State.Loading`.

Remote images never load: an `ImageTransformer` reserves the measured document width on frame one
and both image slots render `MarkdownBrokenImage` (icon plus alt text resolved via
`LocalMarkdownSource`).

`ConfirmDialogBreeze` takes a value (`ConfirmBody` with `Body.Text`, `Body.Markdown`,
`Body.Empty`, or `Body.Custom`) through `ConfirmDialogState.setConfirm`. It hoists the markdown
parse state so fetching and parsing share a single spinner in place of the body and keep confirm
disabled until the document is ready. Empty changelogs distinguish `NotConfigured` (`LinkOff`),
`Empty` (`Draft`), and `Failed` (`CloudOff` with retry).

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no kernel ioctl involved |
| 2 Feature ids | not applicable | `KSU_FEATURE_MAX` untouched |
| 3 Strings | six added in both `values/` and `values-zh-rCN/` | `markdown_copy_code`, `markdown_code_copied`, `module_changelog_not_configured`, `module_changelog_empty`, `module_changelog_load_failed`, `module_changelog_retry` |
| 4 Manifest / gradle | removed `commonmark` x5, added the two renderer artifacts | `grep -rn commonmark manager/` returns nothing |
| 5 Breeze pages | none removed | dialog call sites updated to `ConfirmBody` |
| 6 supercall wrappers | not applicable | `ksucalls.rs` and `ksu.cc` untouched |
| 7 ksud CLI | not applicable | no subcommand changes |
| 8 Module parameters | not applicable | bootconfig keys untouched |
| 9 Build workflows | the 10 Markdown WebView assets removed; the directory stays | `colors_dark.css`, `colors_light.css`, `font.css`, `markdown.css`, `markdown.js`, `suggested_changes.css`, `syntax_dark.css`, `syntax_light.css`, `template.html`, `template_dark.html` are gone; `assets/webview/download.js` remains because `WebViewHelper` injects it into every Module WebUI, which the fork keeps |
| 10 Seams | updated | [seams.md](../seams.md) records the rewritten markdown and dialog files |

## Verified

```
:app:assembleDebug                 BUILD SUCCESSFUL
:app:updateDebugScreenshotTest     BUILD SUCCESSFUL, 8 previews
spec gates                         9/9
```

Eight screenshot previews at 360dp (312dp dialog text slot): inline and block compatibility
corpora, realistic changelog in light and dark, the confirm dialog awaiting and settled, and the
three empty-changelog states plus their dialog shell.

## Sync risk

Upstream still ships the WebView renderer, so upstream markdown changes target deleted files
(`GithubMarkdown.kt`, and the ten `assets/webview/` Markdown files). `assets/webview/` itself is
not deleted: `download.js` belongs to the Module WebUI, and upstream's `bb5f9faaa` added to it
without touching the pruned renderer. The remaining contact points are `LazyMarkdownContent` in
`MarkdownDialogBody` and `ConfirmBody.Body.Markdown`.

The renderer (`com.mikepenz`, pinned at 0.46.0 in `libs.versions.toml`) may rename model
interfaces on a 0.x bump; holding the library's own bundles in `BreezeMarkdownStyle` makes the
compiler point at every affected site.

## Rollback

F2 has no commit of its own — it is part of the one Manager rewrite commit, so `git revert`
would undo the whole five-layer architecture along with the renderer swap. A targeted rollback
takes the commit's markdown slice:

```
git show "$(git log --grep='^manager: rebuild the Manager' -1 --format=%H)" -- 'manager/app/src/main/java/me/weishu/kernelsu/ui/component/breeze/markdown/**' \
                          'manager/app/src/main/assets/webview/**' | git apply -R
```

Restoring the WebView path brings back the ten `assets/webview/` Markdown files,
`GithubMarkdown.kt`, `commonmark` x5, and the handle-based `ConfirmDialogVisuals` API. Nothing
here is ABI or on-disk state.
