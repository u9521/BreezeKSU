# Fork asset inventory

The contact surface between fork and upstream. Two parts, both checked during sync step 6 and cross-checked before every prune:

- **Modified upstream files** — files upstream owns where the fork has edits. An upstream rewrite can silently drop the edit.
- **Added fork trees** — files upstream does not have. The risk is not loss but duplication: upstream eventually grows something similar, and the tree ends up with both.

Last verified against the recorded baseline: 13 modified files. Re-derive against `upstream_base`, not against `main` — `main` carries fork documentation too, so it is not a clean upstream mirror:

```bash
base=$(sed -n 's/^upstream_base: *\([0-9a-f]\{7,\}\).*/\1/p' PRUNING.md)
git diff --name-status "$base" dev | grep -v '^A'
```

## Part 1 — modified upstream files

`Depends on` names the symbols the seam pulls in. It exists so a prune can grep one column instead of re-reading each file: if a pruning target appears there, the seam breaks and must be updated in the same change.

| # | File | What the fork changed | Depends on | When upstream overwrites it |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `kernel/Kbuild` | package name and signing constants | `KSU_PACKAGE_NAME` / `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH` | protected constants: keep the fork values |
| 2 | `manager/app/build.gradle.kts` | package/app name plus dependencies (navigation3, compose.adaptive, haze) | `defaultManagerPackageName` / `defaultManagerName` | brand lines keep fork values; dependency lines accept upstream additions, keep both on conflict |
| 3 | `manager/settings.gradle.kts` | `rootProject.name` `KernelSU` → `BreezeKSU` | — | keep the fork name; upstream never touches this line |
| 4 | `manager/.../ui/MainActivity.kt` | **S1 seam**: early return into `BreezeRoot` when `BreezeMode.rememberIsEnabled()` | `BreezeRoot`, `BreezeMode`, and every screen it registers — currently `ModuleRepoScreen`, `ModuleRepoDetailScreen`, `ScriptRunScreen`, `ColorPaletteScreen` | keep the seam; take upstream's other changes |
| 5 | `manager/.../screen/settings/SettingsMaterial.kt` | **S2 seam**: one line adding `BreezeSettingsRowMaterial()` | `BreezeSettingsRowMaterial`, `R.string.settings_profile_template` | keep the seam |
| 6 | `manager/.../screen/settings/SettingsMiuix.kt` | **S2 seam**: same line | same as row 5 | keep the seam |
| 7 | `manager/.../ui/webui/SuFilePathHandler.java` | `ui_mode` check accepts `"breeze"` | the `ui_mode` preference key | keep the fork's check |
| 8 | `manager/.../ui/webui/WebUIActivity.kt` | **S4 seam**: early return into `BreezeWebUIRoot` | `BreezeWebUIRoot` | keep the seam |
| 9 | `manager/app/src/main/res/values/strings.xml` | the Breeze strings | every `breeze_*` key | both sides are additions: keep both |
| 10 | `manager/app/src/main/res/values-zh-rCN/strings.xml` | same | same as row 9 | keep both |
| 11 | `manager/gradle/libs.versions.toml` | `navigation3`, `haze` and friends | `navigation3`, `haze` | keep both |
| 12 | the repository's root agent entry file | rewritten wholesale as the fork's entry projection | the architecture pages and skills it links to | the fork owns this file; upstream edits are read for content, not applied |
| 13 | `.gitignore` | the fork's own ignore entries | — | merge by intent: upstream additions land, fork additions stay |

## Part 2 — added fork trees

| Path | What it is | Depends on | Duplication risk during sync |
| :--- | :--- | :--- | :--- |
| `manager/app/src/main/java/me/weishu/kernelsu/breezeui/` | the entire Breeze UI skin: screens, components, icons, navigation, previews | the upstream UI contract: `UiMode`, `SettingsRepository` keys, `Natives.Profile`, each ViewModel's UiState | upstream never touches it; the risk is upstream growing a component the skin already implements, leaving two versions and the fork's one drifting |
| `doc/` | the fork ledger: handoff, pruning table, entries, function map, glossary, decisions, sync records | nothing — pure documents | none; upstream has no `doc/`, and its `docs/` holds translated READMEs only |
| the agent framework directory | skills, architecture pages, gates | the ledger under `doc/` | none; upstream carries only a root agent entry file |

## Pruning cross-check

Before deleting anything, walk Part 1 and Part 2 and ask of each row: **does this seam reference a symbol I am about to remove?**

The answer is not obvious from the table alone, which is why `Depends on` exists:

```bash
# for each symbol S being pruned, and each path P in this inventory:
rg -n "$S" "$P"
```

A hit means the seam breaks when the target goes. It belongs in the pruning entry's residue table with the fix spelled out — usually "drop that import and its route", occasionally "relocate the seam". Leaving it to the compiler only catches the Kotlin half; a `Kbuild` constant or a `strings` key fails silently.

After the prune, update the affected rows here: a seam whose dependency was removed is no longer "keep the seam" but "seam removed" or "seam relocated to X".

## Why there are so few seams

The `breezeui` design deliberately avoids adding a constant to upstream's `UiMode` enum. Kotlin requires `when (uiMode)` to be exhaustive, so a new constant would force a Breeze branch into every such site upstream, and any enum reshuffle would break them all at once. The skin selects itself through the `ui_mode` string preference taking the value `"breeze"` instead.

That is why the modification list is short: fork features live in new files, and only a handful of entry points are touched. It is a property worth preserving, but it is not a rule — the fork may modify upstream files freely when that is the clearer change. What it may not do is modify one without recording it here, because the next sync would then have no way to notice the edit vanishing.

## When upstream rewrites a seam

If upstream restructures a function containing a seam, relocate the seam into the new structure rather than reverting upstream's refactor. A reverted refactor creates a conflict that repeats every sync; a relocated seam costs one edit.
