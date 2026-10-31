---
status: modify
mechanism: swap
residue-rev: 1
---

# F3 — reactive settings and a pure data layer

Splits the Manager into layers again: `data/` becomes pure Kotlin, all settings travel through
one observable repository, and the non-UI half of `ui/` moves down into it. No upstream feature
is removed; this entry records the shape the fork's `manager/` now has.

## Why

`data/` imported `ui/`, and `ui/util/` held the CLI, the services and the parsers. The cycle was
invisible while everything compiled, but it meant a "repository" could reach a Compose
`ImageVector` and a "model" could annotate itself `@Immutable` — so no layer could be reasoned
about, let alone tested, without the UI present.

Settings had the same problem from the other side. `SettingsRepositoryImpl` was a bag of
`var` properties, each a fresh `SharedPreferences` read, and every ViewModel constructed its
own. Two screens that each built a repository held two views of the same file: writing in one
did not notify the other, and `MainActivityViewModel` worked around it by registering a
`SharedPreferences` listener and re-reading a hardcoded key set.

One process-wide repository with a `StateFlow<UiSettings>` and an atomic `update` removes both
problems. Readers observe one value; writers persist only the keys a transform actually changed.

## Surface

```
kernel/           — none
userspace/ksud/   — none
manager/          — added: data/UiSettings.kt, data/ksu/ (14 files moved in),
                    data/service/ (4 files moved in), data/model/FlashIt.kt,
                    data/ksu/ManagerVersion.kt
                    rewritten: data/repository/SettingsRepository(Impl).kt,
                    data/repository/SuperUserRepository(Impl).kt, KernelSUApplication.kt,
                    ui/theme/Theme.kt
                    moved within ui/: util/module/Shortcut.kt → screen/module/
                    removed: the ui/util/module/ directory
uapi/             — none
build / CI        — manager/app/src/main/AndroidManifest.xml (DownloadService package)
```

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no kernel-side change |
| 2 Feature ids | not applicable | no kernel-side change |
| 3 Strings | not applicable | no key added, renamed or removed |
| 4 Manifest / gradle | **changed** | `.ui.util.DownloadService` → `.data.service.DownloadService`; the class moved, so the attribute had to follow or the service would not start |
| 5 Breeze pages | none removed | every screen kept its `*Breeze.kt`/`*Entry.kt` pair; their bodies changed to read `UiSettings` |
| 6 supercall wrappers | not applicable | `Natives.kt` lost only its `@Immutable` and a `ui.util` import |
| 7 ksud CLI | not applicable | `KsuCli` moved package, same commands |
| 8 Module parameters | not applicable | untouched |
| 9 Build workflows | not applicable | no asset or CI step |
| 10 Seams | **updated** | [seams.md](../seams.md) row 7 (`ui_mode`) and a new row for the manifest |

## Verified

```
grep -rn -E "^import (me\.weishu\.kernelsu\.ui|androidx\.compose)" \
  manager/.../kernelsu/{data,core,magica,profile} \
  manager/.../kernelsu/{KernelSUApplication,Natives,Kernels}.kt
  → no matches

grep -rn "SettingsRepositoryImpl(" manager/app/src
  → class declaration + KernelSUApplication.kt:48 only

./gradlew :app:compileDebugKotlin :app:compileDebugScreenshotTestKotlin  → BUILD SUCCESSFUL
android_studio.build_project                                        → isSuccess: true, 0 errors
the spec gates                                                     → 9 gates pass
```

## Sync risk

`SettingsRepositoryImpl` and `SettingsRepository` both exist upstream with a different shape —
a set of `var` properties, plus `uiMode`. An upstream change to either file is not mergeable;
read it for the key it adds or the feature call it makes, then re-express it as a `UiSettings`
field and a `writeChanged` branch, keeping the preference key verbatim so installed apps keep
their settings.

`KernelSUApplication` upstream owns the OkHttp client and the predictive-back flag and holds
`ViewModelStoreOwner`. The fork keeps the first two, drops the third, and adds the four
repository singletons. An upstream edit that reintroduces `appViewModelStore` is a signal that
some screen wants an application-scoped ViewModel — give that screen a repository instead of
restoring the store.

A file moving from `ui/util/` to `data/ksu/` breaks an upstream edit silently: upstream still
has the old path and will keep editing it. When a sync brings a change to a file under
`ui/util/` that no longer exists there, check whether it is one of the moved set before
concluding the file was deleted.

## Rollback

F3 is part of the one Manager rewrite commit, so `git revert` undoes the whole architecture.
A targeted rollback reverses that commit's `data/` slice:

```
git show "$(git log --grep='^manager: rebuild the Manager' -1 --format=%H)" -- 'manager/app/src/main/java/me/weishu/kernelsu/data/**' | git apply -R
```

Preference data needs no migration: the keys are unchanged, and
`ColorMode.fromValue` accepts the legacy 0..6 range, so a build that comes back reading
`themeMode: Int` finds exactly what the old build wrote.
