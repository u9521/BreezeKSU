---
status: prune
mechanism: block delete
residue-rev: 1
---

# K9 — module preinit

Runs a module's `preinit/preinit.conf` actions before SELinux policy is loaded. The kernel half is a static nolibc ELF embedded in `kernelsu`, swapped in for PID 1's `init selinux_setup` execve. Maps to upstream `K9` (kernel) and `U3.6` (ksud sync).

## Why

The runner is the fork's largest escalation surface that nobody asked for: 987 lines of static program embedded in the kernel module, holding `insmod` and `exec` capability, reachable before SELinux exists at all. Its kernel half additionally costs a branch on every `execve` in the system — the hook checks `current->pid == 1` on the hot path whether or not preinit is compiled in.

Nothing in the fork used it. The feature shipped upstream on 2026-10 and the fork never grew a module that depends on it, so removing it costs no capability a user had.

The removal is deliberately asymmetric: the kernel side goes completely, the userspace side keeps its shape. A `ksud module preinit` invocation must still succeed, and the call sites in `module.rs`, `init_event.rs` and `cli.rs` must not move.

## Surface

```
kernel/           — deleted preinit/ (blob.S, preinit.c, preinit.h, runner/ incl. vendored nolibc);
                    Kbuild: the runner build rules and the KSU_NO_PREINIT fallback;
                    Makefile: the nolibc format exclusion;
                    core/init.c: two calls and one include;
                    hook/syscall_event_bridge.c: the execve swap, its comment, its include,
                    and now-unused <linux/fcntl.h>;
                    runtime/ksud_integration.c: two ksu_preinit_release() calls;
                    supercall/dispatch.c: the flag, its comment and the include
userspace/ksud/   — preinit.rs reduced to no-ops keeping every pub fn signature;
                    module.rs: doc comment only (regenerate_preinit keeps its name and body)
manager/          — data/model/Module.kt: the preinit field;
                    data/repository/ModuleRepositoryImpl.kt: its parse line
uapi/             — supercall.h: KSU_GET_INFO_FLAG_PREINIT marked reserved, version-6 comment
build / CI        — none
```

## Residue

| Surface | Verdict | Evidence |
| :--- | :--- | :--- |
| 1 ABI command numbers | not applicable | no ioctl added or removed; `KSU_IOCTL_LOOKUP_SYMBOLS` stays registered |
| 2 Feature ids | not applicable | preinit was a get-info flag, not a feature id; `KSU_FEATURE_MAX` unchanged |
| 3 Strings | not applicable | `ksud module preinit status|reset` still parse; no Manager string mentioned preinit |
| 4 Manifest / gradle | not applicable | no component, no dependency |
| 5 Breeze pages | not applicable | no page rendered the field; it reached the UI nowhere |
| 6 supercall wrappers | **hit** | `KSU_GET_INFO_FLAG_PREINIT` kept at `1U << 5`, never reused; `ksucalls.rs::is_preinit_supported()` still compiles and now always reads false, because the kernel never sets the bit |
| 7 ksud CLI | **hit** | `module preinit status` / `reset` keep their handlers; `--no-preinit` keeps writing `preinit=0` into `ksu_config` — the kernel now ignores an unknown parameter with a `pr_warn` and loads anyway, and the same `ksu_config` mechanism is reused when patching other LKMs |
| 8 Module parameters | **hit** | the kernel-side `module_param_named(preinit, ...)` is gone. `boot_patch.rs` deliberately keeps writing the key: for a kernel that no longer declares it, `unknown_module_param_cb()` warns and continues (`kernel/module/main.c`), so an old ramdisk carrying the key still boots |
| 9 Build workflows | **hit** | the `KSU_UAPI_INC` probe and the `RUNNER` custom command are gone; CI needs no change because it never invoked them directly |
| 10 Seams | not applicable | no row in [seams.md](../seams.md) names preinit; the seam that would have needed one (kernel↔ksud for the runner) is deleted at both ends |

## Verified

```
rg -i preinit kernel/                    → no match
cargo build --target x86_64-linux-android → Finished, 0 warnings
cargo build --target aarch64-linux-android → Finished, 0 warnings
the spec gates                           → all ten pass
```

The kernel half is not built locally — no GKI tree here. `kernel/preinit/` was self-contained apart from the five call sites listed above, all of which are deleted, so the risk is that a symbol the fork still references went with it. `rg -i preinit` returning nothing across `kernel/` is the check that covers this.

## Sync risk

`upstream` still carries preinit, so every future baseline advance re-introduces the whole directory. The paths are already in `removed_paths`; what a sync must additionally watch is `kernel/Kbuild`: upstream's runner block sits immediately above `obj-$(CONFIG_KSU) += kernelsu.o`, and a rebase that keeps the fork's edit while taking upstream's side re-adds it without a conflict.

The second risk is quieter. `ksu_preinit_handle_execve()` sat in the `execve` hook; upstream may add another `current->pid == 1` special case there, and it will apply cleanly into a file the fork has already edited one hunk above.

`userspace/ksud/src/preinit.rs` is a stub upstream will keep editing. The stubs must keep their exact signatures or the call sites break — do not "helpfully" re-sync the file.

## Rollback

Revert this commit; `kernel/preinit/` comes back from upstream with its build rules. Because `KSU_GET_INFO_FLAG_PREINIT` was reserved rather than removed, a rollback restores the same bit value — no ABI migration, and no version bump is needed either way.
