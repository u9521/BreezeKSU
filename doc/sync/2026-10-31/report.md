# Sync report — 2026-10-31 baseline advance

`75e179d3` → `51c21786` (v3.3.1-25 → v3.3.1-32). Mode: **baseline advance**, not a re-freeze — none of the four triggers in [decisions/README.md](../../decisions/README.md) §6 fired: UAPI unchanged at 6, seven upstream commits touching 46 files, no KMI or toolchain movement.

## Commits

| Upstream | Subject | Paths | Route |
| :--- | :--- | :--- | :--- |
| `10efe6648` | Fix ads | `website/docs/.vitepress/config.ts` | inherited — the AdSense client id in the `head` script, `ca-pub-8356785667482909` → `ca-pub-2610070972052494`. It is the same upstream ad slot the 2026-10-27 report flagged under "Needs human"; inheriting it is not a decision to keep it |
| `db6263d3b` | fix(kernel): align SELinux hide policy parsing (#3936) | `kernel/feature/selinux_hide.c` | inherited — the fork never touched this file, so the port is the upstream text verbatim: one parse of the backup policy with the live policy checked in the same pass, an `-ESTALE` retry against a reloaded policy, and no live-SID sync when backup conversion fails |
| `bb5f9faaa` | manager: add File interface to WebUI (#3732) | `js/`, `webview/download.js`, `webview/file/*`, `DownloadManager`/`DownloadService`, `WebViewInterface`/`WebViewHelper`/`WebUIState`, `filepaths.xml` | inherited with two hand resolutions — see "removed_paths, decided by a human" |
| `ab05a0626` | fix: add new pm interface (#3955) | `js/`, `SuperUserViewModel.kt`, `webview/pm/KsuPm.kt`, `WebViewHelper`/`WebViewInterface` | inherited — the `loadApps` scan cache joins the fork's `setSearchText` in the same file; both kept |
| `7bab12df9` | js: auto publish npm (#3956) | `.github/workflows/npm.yml` | inherited — a new workflow the fork had no copy of |
| `446c29208` | website: add llms.txt for AI agents | `website/docs/.vitepress/{config,seo}.ts`, every `guide/*.md`, `robots.txt`, `package.json`, `yarn.lock` | inherited — additive site plumbing; the fork's own docs pages are untouched |
| `51c217863` | ci: pin website deploy to Node LTS | `.github/workflows/deploy-website.yml` | inherited — `node-version: latest` → `lts/*` for the docs build |

Two of the seven land inside `removed_paths`; both were resolved by a human decision recorded below, not by the sync.

## How it landed

```bash
git fetch upstream --prune --tags
git rebase --onto 51c21786 75e179d3 dev
```

Six fork commits replayed. Four replayed clean and `range-diff` as `=`; two needed hand work:

- **`61e20f80b` "reduce the Manager to a single Breeze renderer"** — modify/delete on `RebootListPopupMiuix.kt`. `git rm`; the file stays deleted. `WebUIScreen.kt` auto-merged and kept both `bb5f9faaa`'s `deviceInsets` union and the fork's single `UiMode.Breeze -> HandleWebUIEventBreeze(webUIState, fileLauncher)` branch, so nothing was edited by hand there.
- **`cafee839a` "remove the module repository and its registry entirely"** — content conflicts in `DownloadManager.kt` and `DownloadService.kt`, where `bb5f9faaa` had just added the WebUI download path to the same lines the registry removal was deleting. Resolved by keeping every WebUI/local-save/OPEN_FILE addition (`targetPath`, `mimeType`, `cookie`, `userAgent`, `completionAction`, `deleteFileOnDismiss`, `registerLocalSave`, `resolveDownloadMimeType`) and dropping every `data.registry.*` symbol (`ExpectedFile`, `DownloadVerificationException`, `RegistryDownloadStore`, `RegistryUrls`, `canShareDownload`, `downloadVerified`, `registryClient`, `REGISTRY_DOWNLOAD_MAX_AGE_MS`, and the two `EXTRA_EXPECTED_*` keys). The deduplication predicate in `enqueue` now reads:

  ```kotlin
  it.fileName == fileName &&
      it.targetPath == targetPath &&
      it.completionAction == completionAction &&
      it.url == url &&
      (it.status == Status.PENDING || it.status == Status.DOWNLOADING)
  ```

  which is the fork's pre-sync predicate plus the two new identity fields `bb5f9faaa` introduced, with `canShareDownload(url, expected, …)` gone rather than stubbed.

`git diff --check` was clean before each `git rebase --continue`. `git range-diff 75e179d3..694c71e2d 51c21786..HEAD` reports `=` for four of six and `!` for the two above, whose diffs are the resolutions just described.

One fork commit sits on top of the rebase: **`manager: include display cutout in BreezeWebUIRoot insets`** — see below.

## removed_paths, decided by a human

`bb5f9faaa` touched two pruned paths. Both were put to a human before the rebase and answered as follows.

1. **`manager/app/src/main/assets/webview/download.js` — keep it.** The whole `assets/webview/` directory was in `removed_paths`, but F2 only ever deleted the ten Markdown WebView files; `download.js` is Module WebUI plumbing, and `WebViewHelper.loadDownloadJs` still reads it for every module page the fork renders. Narrowing the entry to the ten files is the correction: the directory-level entry was broader than the pruning it recorded, and would have flagged the next legitimate `download.js` edit as a restored feature. `doc/PRUNING.md` now lists the ten paths, and [F2](../../entries/F2-compose-markdown.md) says "the ten Markdown WebView files" instead of "the directory is gone".
2. **`manager/.../component/rebootlistpopup/RebootListPopupMiuix.kt` — drop it.** Miuix was pruned in F1; `bb5f9faaa` only changed a `DropdownItem` import inside a file the fork had already deleted. The rebase's modify/delete is resolved by keeping it deleted, which is the same answer F1 gave.

## `BreezeWebUIRoot.kt` cutout sync

`bb5f9faaa` changed `WebUIScreen.kt`'s reported insets from `WindowInsets.systemBars` to `WindowInsets.systemBars.union(WindowInsets.displayCutout)`, so a module page laid out to those numbers clears a notch. The rebase carried that change into `WebUIScreen.kt` automatically, but `BreezeWebUIRoot.kt` is a fork-owned copy of the same host and kept measuring bare `systemBars` — the fork would have reported insets that disagree with upstream's own screen on the same device. `BreezeWebUIHost` now holds `val deviceInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)`, keys the snapshot flow on it, and the `displayCutout`/`union` imports came with it. Committed separately as `manager: include display cutout in BreezeWebUIRoot insets`.

**Why [ledger.md](../ledger.md) gains no row.** The ledger exists to stop a later big sync porting the same feature twice: a hand-merged port is not recognised by patch-id, so without a row it would be replayed on top of the code that already contains it. Neither of this session's two deltas is that shape. `bb5f9faaa` is inside `upstream_base = 51c21786` — it arrived by baseline advance, not by a hand port, and the next sync's replay window starts at `51c21786`, so it is not in the queue at all. The cutout commit is a one-line follow-on *on top of* that advance: its upstream source (`bb5f9faaa`) is likewise behind the base, so nothing will replay it either. A ledger row would name a SHA the sync no longer replays. The rule the ledger encodes — "append a row for a feature landed by hand from upstream" — is not triggered, and adding a row here would make the table claim a queue removal that does not exist.

## Anchors

- `doc/PRUNING.md`: `upstream_base` → `51c21786`, `upstream_base_advanced` → `2026-10-31`; `removed_paths` narrowed from the `assets/webview/` directory to its ten Markdown files; "Most recent" / "Previous" rolled forward.
- `doc/FUNCTION_MAP.md`: baseline line → `v3.3.1-32-g51c217863`; K5.5 line count 1254 → 1268 (`db6263d3b`); M12 WebUI 1678 → 2863 lines, with the `js/` File and PM interfaces, `webview/file/*`, `webview/pm/KsuPm.kt` and `BlobDownloadHandler` named, and the subsystem estimate 2.7k → 3.9k.
- `doc/HANDOFF.md`: baseline row → `51c21786` (v3.3.1-32).
- `doc/seams.md`: three rows corrected, none of them by the sync's own diff — row 4 named a deleted `IntentDispatcher` where the live symbol is `BreezeIntentRouter` (row 8's counterpart for `BreezeWebUIRoot` was already right), row 9 claimed the fork's strings carry a `breeze_*` prefix and they carry none (29 added keys, all unprefixed), and the markdown row's "an addition to a directory that is gone" now says files, because `assets/webview/` itself survives.
- `removed_symbols`, `uapi_anchor`, `residue_rev`, `protected_constants`: untouched — no pruning decision changed, and `db6263d3b` is a rewrite of a file the fork never edited.

## Gates

| Gate | Result |
| :--- | :--- |
| Spec gates (`verify-all`) | green — 9/9 |
| G1 pruning integrity | green — all 18 `removed_paths` absent; `assets/webview/download.js` present; every `Depends on` symbol in `seams.md` resolves; `protected_constants` hold the fork values (`u9521.breezeksu`, `BreezeKSU`, `0x01ed`, `3ebb1bb9…`) |
| G2 UAPI agreement | green — `KERNEL_SU_UAPI_VERSION = 6` in `uapi/supercall.h`, re-exported by `ksucalls.rs`'s `ksu_uapi::KERNEL_SU_UAPI_VERSION` binding and by the JNI bridge's `get_manager_uapi_version()` in `ksu.cc`, which `Natives.managerUAPIVersion` reads; `MINIMAL_SUPPORTED_KERNEL = 32513`. `uapi/` is untouched by the seven commits, and `KSU_FEATURE_MAX` is unchanged at 7 with id 5 still deliberately unused |
| G3 ksud Android compile | skipped — no Rust source changed; the seven commits touch `kernel/`, `manager/`, `js/`, `website/` and `.github/` only |
| G4 Manager compile | green — `:app:assembleDebug` through Android Studio, `isSuccess: true`, zero errors |

The `BreezeWebUIHost` / `18 removed_paths` figures above describe the tree at sync commit `f789dec44`, not the tree today: F4 and F5 later deduplicated `BreezeWebUIHost` into `WebUIScreen` and added four architecture `removed_paths`, so a reader counting either now gets different numbers for a reason the sync did not cause.

## Needs human

1. **The ad slot is still upstream's, and now carries a second id.** `10efe6648` changes the AdSense client in `website/docs/.vitepress/config.ts` to `ca-pub-2610070972052494`; the 2026-10-27 report raised the same question about `website/docs/public/ads.txt` (`pub-2610070972052494`) and it is still unanswered. `deploy-website.yml` publishes `website/` from `main`, so the fork's own docs site serves upstream's ad slot. Nothing in this sync makes it worse, and nothing fixes it — keep it, replace the id, or stop shipping both files, a decision for `PRUNING.md`.
2. **`npm.yml` publishes from `main` under upstream's package name.** `7bab12df9` adds a workflow that runs on a push to `main` touching `js/package.json` and, when the version differs from what npm already has, runs `npm publish --provenance` on `js/package.json`'s name — still `kernelsu`, version `3.3.0`. The fork has not renamed it, so a `main` batch that includes `js/` will attempt a publish under a name the fork does not own, and the publish step needs an `id-token` permission and a trusted-publisher registration upstream already has. Either disable the workflow on the fork, gate it behind a tag, or rename the package first — a decision for `doc/decisions/`, not for a sync.

## Not done here

`dev` only. `main` takes this with the next verified batch, and nothing was pushed.
