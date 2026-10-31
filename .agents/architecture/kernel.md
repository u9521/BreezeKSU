# Kernel module

Covers: kernel/, uapi/

The kernel side is a C module that hooks syscalls, exposes the supercall ABI, and enforces per-app policy. It has three load forms — built-in, LKM, and late-load — and the same source must survive all three.

## Never recycle an ABI number

`KSU_IOCTL_*` command numbers and `KSU_FEATURE_*` ids are append-only. To remove a capability, stop registering its handler and leave the number reserved; the caller then receives `-ENOTTY` instead of silently reaching a different function.

Recycling a number breaks devices that still carry an old `/data/adb/ksu/.feature_config` or an old Manager: their id now resolves to a different feature with different semantics, and the mismatch is invisible until behaviour goes wrong in the field.

## Module parameters are a load-time contract

`allow_shell`, `norc`, `bundled`, `block_modules`, `preinit` and `force_feature` are read from bootconfig before the module initialises. Dropping one from the source while a boot image still supplies the key makes module loading fail outright.

Removing a parameter is therefore a two-sided change: first stop `ksud` from writing the key (`boot_patch.rs`'s `ksu_config`), ship that, and only then delete the kernel read.

## The init.rc injection is the module system's only entry point

`kernel/runtime/ksud_integration.c` injects the rc fragment that invokes `ksud` for `post-fs-data`, `services` and `boot-completed`. Delete it and modules stop running entirely — installation still works, so the failure looks like "modules broke", not "a hook was removed".

Treat this file, the supercall base, and the feature framework as load-bearing regardless of what else is pruned.

## Symbol resolution degrades silently

`infra/symbol_resolver.c` and the architecture-specific `patch_memory.c` locate and patch kernel text by symbol. A KCFI suffix change or an export-policy change turns a successful lookup into a failed one without a compile error; the hook then simply stops firing.

Any change here needs an on-device check, not a compile check. Kernel compilation is not gated locally — it needs a target kernel tree — which is why kernel changes travel through the kernel-compat lane and get one device run before landing.

## Flags that already exist before adding new ones

`CONFIG_KSU_DISABLE_MANAGER`, `CONFIG_KSU_DISABLE_POLICY`, `CONFIG_KSU_X86_PATCH_SYSCALL_DISPATCHER`, `KSU_DEBUG`, and the `KSU_PACKAGE_NAME` / `KSU_EXPECTED_SIZE` / `KSU_EXPECTED_HASH` signing constants are the existing trim surface. Reach for one of these before introducing a new configuration axis.

The signing constants are fork-owned: upstream values are never accepted for them.

## Boundaries

- Kernel code never formats user-facing messages; it reaches userspace through supercall responses and events.
- Anything crossing to userspace goes through `uapi/`, never a private header — see [contracts](contracts.md).
- Policy decisions belong in `kernel/policy/`; hooks call into it rather than re-implementing checks.
