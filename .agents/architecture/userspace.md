# Userspace daemons

Covers: userspace/

Two Rust crates: `ksud` (the daemon and CLI that owns modules, boot patching and policy plumbing) and `ksuinit` (the LKM bootstrap init and ELF loader).

## The Android target is the only real check

`cargo check` on the host target proves nothing here: these crates are `android`-gated and link against things the host does not provide. Verify with `cargo ndk -t arm64-v8a check`, or `cross build --target aarch64-linux-android` where cargo-ndk is unavailable.

A change that compiles on the host and not for Android is the normal failure mode, and it fails at the very end of a long pipeline.

## Boot-patch cannot be deleted wholesale

`boot_patch.rs` mixes two things: probing (KMI, slot, partition layout, `boot-info` subcommands) and patching (injecting the LKM and `ksuinit`, repacking, flashing). The Manager's install path, `ksud uninstall`'s boot restore, and `late_load`'s KMI detection all consume the probing half.

Pruning boot patching means lifting the probing into a shared module first, then removing patch and restore. Deleting the file breaks install and uninstall in ways that only show on a device.

## Modules, metamodules, and their ordering

`init_event.rs` runs the boot pipeline: `post-fs-data` → `services` → `boot-completed`. Metamodule scripts run before regular modules at every phase. Only one metamodule may be active, and `metamodule.rs` maintains the `/data/adb/metamodule` symlink that other components read.

When a feature is pruned from the kernel, the corresponding `ksud` entry point has to go in the same change; otherwise the CLI keeps advertising a command whose supercall now returns `-ENOTTY`.

## The Manager needs binaries staged

The Android app build embeds this crate as `libksud.so`, one per ABI the app ships. Build the crate for each ABI in `abiFilters`, copy each result into `manager/app/src/main/jniLibs/<abi>/libksud.so`, then build the app. `jniLibs` is git-ignored, so a fresh checkout must always do this first.

## Boundaries

- `ksud` speaks to the kernel only through `ksucalls.rs`; no other file issues raw ioctls.
- Version checks are fatal by design (`ensure_uapi_version_matched`): a `ksud` that does not match the kernel refuses to run rather than operating under a wrong contract.
- Assets embedded in the binary (busybox, bootctl, waitsys, `*.ko`) have named consumers; removing a consumer means removing its asset here and in the build workflow together.
