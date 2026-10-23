# Manager app

Covers: manager/

A Kotlin/Compose Android app that talks to the kernel through JNI and to `ksud` through the CLI. It carries two UI implementations plus the fork's own `breezeui/` skin.

## Every supercall has three mirrors

A kernel ioctl is mirrored in `userspace/ksud/src/ksucalls.rs` and again in the app's JNI bridge (`manager/app/src/main/cpp/ksu.cc`) with a Kotlin façade in `Natives.kt`. Changing one without the others produces a runtime drift that compiles cleanly on both sides.

Treat "add or change an ioctl" as a three-file change by default.

## Breeze is a third UI mode, not an enum constant

`breezeui/` is selected through the `ui_mode` string preference taking the value `"breeze"`. Upstream decides between Material and Miuix with a `when (uiMode)` that Kotlin requires to be exhaustive; adding an enum constant there would force a breeze branch into every such site, and an upstream enum reshuffle would break them all at once.

The skin therefore hooks in at a small number of seams: `MainActivity` early-returns into `BreezeRoot`, the two settings screens each gain one row, and the web UI activity early-returns into its Breeze root. Keep that shape. A new feature that needs a Breeze surface adds files under `breezeui/` and, at most, one seam line.

## Pruning a feature means pruning its Breeze page

Each prunable feature has a `*Breeze.kt` plus `*Entry.kt` pair under `breezeui/screen/`. Removing the upstream ViewModel or data layer without removing the pair leaves dead references to a class that no longer exists — the app stops compiling, and the error points at generated-looking code.

List both paths in the pruning entry's residue checklist before starting.

## Two ABIs, no toolchain in WSL

The app is built with the Gradle wrapper the repository pins. Building it from WSL commonly fails on the JDK and Gradle versions the wrapper asks for; building it through Android Studio (on Windows) with the repository opened at its Windows path is the reliable route.

`jniLibs` must already contain `libksud.so` for every ABI in `abiFilters` before any Gradle command — see [userspace](userspace.md).

## Boundaries

- The app never issues ioctls directly; all kernel access goes through the JNI bridge.
- `breezeui/` may read upstream app code but upstream app code must not depend on `breezeui/` except at the documented seams.
- String additions go to both `values/` and `values-zh-rCN/`; a key present in one and missing in the other renders as a raw key on some devices.
