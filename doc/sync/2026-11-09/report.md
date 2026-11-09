# Sync report — 2026-11-09 baseline advance

`28db74f6` → `c680b548` (v3.3.1-36 → v3.3.1-42). Mode: **baseline advance**, not a re-freeze — none of the four triggers in [decisions/README.md](../../decisions/README.md) §6 fired: UAPI unchanged at 6, six upstream commits, no KMI or toolchain movement.

## Commits

| Upstream | Subject | Paths | Route |
| :--- | :--- | :--- | :--- |
| `019a3c01` | refactor(kernel): Mark original type datum to save symtab search (#3969) | `kernel/feature/selinux_hide.c`, `kernel/selinux/{rules.c,sepolicy.c,sepolicy.h}` | inherited — the fork has no edit in any of the four |
| `730f6446` | manager: accept x-zip MIME types when opening module zips (#3929) | `manager/app/src/main/AndroidManifest.xml`, `manager/.../ui/navigation3/IntentDispatcher.kt` | **split**: the manifest half inherited; the dispatcher is a `removed_paths` entry, so its half was re-expressed in `ui/nav/BreezeIntentRouter.kt` |
| `ca346659` | fix(kernel): parse setprocattr context once in selinux hide (#3981) | `kernel/feature/selinux_hide.c` | inherited |
| `9ae6e65a` | fix(kernel): Fix arm64 syscall first param register on 7.3+ (#3983) | `kernel/include/arch.h` | inherited |
| `c6f773ce` | docs: clarify current KernelSU installation paths (#3642) | `website/docs/guide/installation.md` | inherited — the fork has no edit in this file |
| `c680b548` | kernel: fix x64 preinit runner ELF alignment for non-4K page sizes (#3933) | `kernel/Kbuild` | **hand**: the hunk sits inside the block the fork deleted for preinit |

## How it landed

```
git fetch upstream --prune --tags
git rebase --onto c680b548 28db74f6 dev
```

The rebase completed without leaving conflict markers, but two of the five fork commits rebuilt to a different tree:

| Before | After | Range-diff |
| :--- | :--- | :--- |
| `24e15963` kernel: remove module preinit | `5ecbcab4` | `!` |
| `a920b88a` ksud: remove preinit, per-app SELinux rules and Magica | `d731dd6c` | `=` |
| `6d4dcd79` website: publish the docs site on GitHub Pages | `4437bed0` | `=` |
| `0e5d9a39` manager: rebuild on Breeze… | `2ed98122` | `!` |
| `c2fea899` docs: record the fork ledger and the spec gates | `6e1faa69` | `=` |

### The two `!`

**`kernel/Kbuild`.** `24e15963` deletes lines 95–138: the runner build block. `c680b548` edits exactly that block, adding `KSU_RUNNER_LDFLAGS` and the page-size comment. Git resolved the overlap by keeping upstream's version, so `kernel/preinit/` stayed deleted while `Kbuild` went on referencing it. DDK runs `gki_defconfig`, which enables `CONFIG_HEADERS_INSTALL`, so `$(objtree)/usr/include/linux/unistd.h` exists and the `ifneq` resolves **true**: `kernelsu-objs` then lists two objects under a directory that does not exist, and the LKM build stops there. Fixed by deleting the block again; the commit now removes 8,522 lines instead of 8,513, the extra 9 being upstream's own additions to a file the fork never builds.

**`IntentDispatcher.kt`.** `730f6446` changes two files. The manifest is upstream's and arrived whole. The dispatcher is under `ui/navigation3/`, deleted by F5-era pruning, so the rebase had no target for the second half: after it, the manifest advertised three zip MIME types while `BreezeIntentRouter` still accepted only `application/zip`. A file manager reporting `application/x-zip-compressed` would hand the app a zip it then ignored. Fixed by adding the same `ZIP_MIME_TYPES` set to the router — the port lands on the fork's equivalent, not on the deleted file.

Both fixes were folded into the commits they belong to (`git rebase --autosquash`), so `dev` still reads as one commit per module.

## Template sync

Separately, the fork follows the `breezexample` template's split-scene rework (`ae526bf` → `c7bfed8` → `cbd6c66`): `BreezeListDetailScene` is now a horizontal strip under a moving cursor instead of two columns, panes pair by `PaneHost`/`PaneDetail` markers rather than by a string binding key, and the ratio comes from a `SplitRatioSource` the composition root provides. That is a fork commit of its own and gets a row in [ledger.md](../ledger.md); it is not part of the baseline.

## Verification

```
the spec gates    → 10 of 10 green
:app:compileDebugKotlin --no-daemon    → BUILD SUCCESSFUL
```

`kernel/Kbuild` is byte-identical to the pre-rebase fork version (`e5f9288c`), so the preinit pruning is intact; `grep -rn preinit kernel/Kbuild` returns nothing.
