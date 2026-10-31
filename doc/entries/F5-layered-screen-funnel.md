---
status: modify
mechanism: swap
residue-rev: 1
---

# F5 — layered screen funnel and co-located view models

Completes the five-layer Manager UI: the component library splits into `common/` and `breeze/`,
every page becomes a `*ViewModel`/`*UiState` → `*Entry` → `*Breeze` funnel, and `ui/viewmodel/`
is dissolved into the pages it served. No upstream feature is removed.

## Why

The five layers existed on paper after F4 but not in the tree. `ui/component/` was one flat
directory, so a skin-agnostic widget and a Breeze-only one were indistinguishable to both the
compiler and a reader. Every `NavEntry` still resolved its own ViewModel through a shared
`ui/viewmodel/` package, which meant the package graph was acyclic only because nothing had
tried to break it — not because something refused.

Three changes make the graph enforceable, and one gate then enforces it:

**Two component groups.** `common/` is the skin-agnostic base; `breeze/` is the design system.
`common/` may not import `breeze/`, so a widget that belongs to the skin cannot quietly become
part of the base.

**One funnel per page.** State (`*ViewModel.kt`, `*UiState.kt`) derives, the entry (`*Entry.kt`)
binds, the skin (`*Breeze.kt`) draws. The first two take zero `ui.component.*` and zero
`material3.*` imports — a rule that pays off immediately, because it is also what makes an entry
testable without a Compose host.

**Co-location.** A ViewModel in `ui/viewmodel/` is reachable from every page and therefore owned
by none. Moving each beside its page makes the funnel visible in a directory listing and makes the
deleted package a structural statement rather than a convention.

## Surface

```
kernel/           — none
userspace/ksud/   — none
manager/          — moved (git rename, contents preserved):
                    ui/viewmodel/{Home,Module,Settings,Sulog,SuperUser,Template}ViewModel.kt
                      → ui/screen/{home,module,settings,sulog,superuser,template}/*ViewModel.kt
                    ui/component/{AppIconImage,KeyEventBlocker,ObserveAsEvents}.kt
                      → ui/component/common/
                    ui/component/{filter,statustag,rebootlistpopup,material}/*
                      → ui/component/breeze/*
                    added: ui/component/breeze/scaffold/BreezeScaffold.kt
                      (BreezeScaffold, BreezeScaffoldScope, BreezeScrollColumn)
                    removed: ui/viewmodel/ (directory), ui/component/ flat files
                    rewritten: every ui/screen/<feature>/{*Entry,*Breeze,*UiState}.kt
uapi/             — none
build / CI        — none
```

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no kernel-side change |
| 2 Feature ids | not applicable | no kernel-side change |
| 3 Strings | not applicable | no key added or removed; labels keep their existing ids |
| 4 Manifest / gradle | not applicable | no component or dependency change |
| 5 Breeze pages | **reshaped, none removed** | every page keeps its triple. Entries lost their imperative handles (`SnackbarHostState`, `SliderState`, `rememberConfirmDialog`) to `*Breeze.kt` or to `BreezeScaffold`; `UiMessage` carries one-shot feedback |
| 6 supercall wrappers | not applicable | untouched |
| 7 ksud CLI | not applicable | untouched |
| 8 Module parameters | not applicable | untouched |
| 9 Build workflows | not applicable | no asset or CI step |
| 10 Seams | **updated** | [seams.md](../seams.md): the relocated `data/ksu` and `data/service` files, the six ViewModels and `Shortcut.kt` are recorded as moved; the Part 2 `ui/` row now names the five layers |

## Verified

```
verify-arch-layers  → ✓ 296 Kotlin files respect the five-layer one-way dependency graph
verify-all          → all ten gates pass
grep -rn "me.weishu.kernelsu.ui.viewmodel" manager/app/src  → no matches
android_studio.build_project → isSuccess: true (0 errors)
```

The gate was negative-tested: a placeholder under `ui/viewmodel/`, a `data/` file importing
`androidx.compose.runtime`, a flat `ui/component/` file importing `data`, a `Home*Entry.kt`
importing `material3`, and a `ui/nav/` file importing `ui/screen/` each produced exactly one
reported violation, and the clean tree reports none.

## Sync risk

`ui/viewmodel/` is an upstream directory. A future upstream file placed there compiles nowhere
useful in this tree — the gate rejects it outright, which is the intended early failure. When
upstream adds a ViewModel, the port is "write it beside the page", and the screen that reads it
must still keep its entry free of `material3` imports.

Upstream's `ui/component/` also has a flat shape. A ported component lands in `common/` when it
knows nothing about the Breeze skin and in `breeze/` otherwise; picking the wrong one fails the
gate rather than the build, so the mistake surfaces at the next `verify-all`.

## Rollback

F5 is part of the one Manager rewrite commit, so `git revert` undoes the whole architecture.
A targeted rollback reverses that commit's page and component slice:

```
git show "$(git log --grep='^manager: rebuild the Manager' -1 --format=%H)" -- 'manager/app/src/main/java/me/weishu/kernelsu/ui/screen/**' \
                          'manager/app/src/main/java/me/weishu/kernelsu/ui/component/**' \
                          'manager/app/src/main/java/me/weishu/kernelsu/ui/viewmodel/**' | git apply -R
```

`ui/viewmodel/` is recreated from the six renamed files — the
renames carried their contents, so no ViewModel body is lost — and `verify-arch-layers` is
switched off in the gate config if the intermediate shape must persist. No persisted state
changes: preference keys and navigation keys are untouched.
