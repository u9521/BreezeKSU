# Manager app

Covers: manager/

A Kotlin/Compose Android app that talks to the kernel through JNI and to `ksud` through the CLI. It has **one** UI implementation: Breeze, under `ui/`. Upstream's Material and Miuix renderers were removed in F1 — see [F1](../../../doc/entries/F1-single-ui.md).

The constraints are split by what they are about, because each is read at a different moment:

| Page | Read it when |
| :--- | :--- |
| [build](build.md) | building, or changing the toolchain, NDK/CMake flags or the staged daemons |
| [ui](ui.md) | adding a screen, a dialog, an icon, or a preview |
| [overlay](overlay.md) | building a modal, or changing how the page behind one is blurred |
| [overlay stack](overlay-stack.md) | stacking two modals, or changing how a card or scrim animates in |
| [markdown](markdown.md) | touching the changelog renderer or its style |

## Every supercall has three userspace mirrors

A kernel ioctl (`uapi/supercall.h`) is mirrored in `userspace/ksud/src/ksucalls.rs`, in the app's JNI bridge (`manager/app/src/main/cpp/ksu.cc`), and in the Kotlin façade (`manager/app/src/main/java/me/weishu/kernelsu/Natives.kt`). Changing one without the others produces a runtime drift that compiles cleanly on both sides.

## Boundaries

- The app never issues ioctls directly; all kernel access goes through the JNI bridge.
- There is no second UI to fall back on: breaking a Breeze screen breaks the app.
- String additions go to both `values/` and `values-zh-rCN/`; a key present in one and missing in the other renders as a raw key on some devices.
