# KernelSU function map — pruning baseline

> Baseline: `v3.3.1-22-g6d31a6137` · This document describes what upstream is and where it lives. It does not change code.
> Purpose: a complete, tickable inventory of upstream subsystems, so pruning can be decided one ID at a time.

## 0. Scale

| Layer | Size | Notes |
| :--- | :--- | :--- |
| Kernel driver `kernel/` | 43 `.c`, ~14.1k lines; 22.6k with headers | C, three load forms: GKI / LKM / late-load |
| UAPI `uapi/` | 6 headers, 364 lines | the only kernel↔userspace contract, **versioned** |
| ksud `userspace/ksud` | 31 `.rs`, 14.0k lines + `installer.sh` 493 lines | Rust daemon/CLI on Android |
| ksuinit `userspace/ksuinit` | 704 lines Rust | LKM bootstrap init + ELF loader |
| Manager `manager/` | 244 Kotlin/Java, 42.4k lines; 877 lines C++ | Compose + JNI + 46 `values-*` locales |

Two commonly misplaced features:

- **"Update package analysis"** is not in the kernel: OTA `payload.bin` / factory image parsing lives in `manager/core/tasks/` (`Payload.kt`, `ExtractImage.kt`, `BootKernelVersion.kt`), download-and-patch in `manager/ui/util/KsuCli.kt`, and the actual patching in `ksud/boot_patch.rs` and `ksud/lkm_image.rs`.
- **"Magica escalation"** is not in the kernel either: `ksud/magica.rs` (adb root plus `resetprop` on `ro.debuggable`) with `manager/magica/` (ZygotePreload / isolated service / boot receiver) and `manager/cpp/adbroot.cc` (injecting into adbd).

## 1. Overview: four layers × subsystems

```text
KernelSU
├─ K  Kernel driver (kernel/ + uapi/)
│  ├─ K1  Lifecycle and load forms        core/init.c
│  ├─ K2  Hijack infrastructure           hook/ (syscall hooks, dispatcher, hot patch, LSM, process marking)
│  ├─ K3  Kernel ABI (supercall)          supercall/ + uapi/supercall.h  ← 25 ioctls
│  ├─ K4  Policy and data                 policy/ (allowlist / app_profile / feature)
│  ├─ K5  Optional feature modules        feature/ (sucompat / kernel_umount / sulog / adb_root / selinux_hide / shell_su_adb_only / module_blacklist)
│  ├─ K6  SELinux subsystem               selinux/ (ksu domain / dynamic policy patches)
│  ├─ K7  Manager validation              manager/ (apk_sign / throne_tracker / pkg_observer)
│  ├─ K8  Boot injection and events       runtime/ (init.rc injection / ksud detection / safe mode / boot events)
│  ├─ K9  preinit runner                  preinit/ (embedded static ELF, runs before SELinux loads)
│  ├─ K10 Common infrastructure           infra/ (symbol resolution / fd wrapping / event queue / mount ns / seccomp cache)
│  └─ K11 Build-time switches             Kconfig / Kbuild
│
├─ U  Userspace daemon ksud (userspace/ksud)
│  ├─ U1  CLI and multicall dispatch      cli.rs (22 subcommands) + argv[0] aliases (su/resetprop)
│  ├─ U2  Boot event pipeline             init_event.rs (post-fs-data / services / boot-completed)
│  ├─ U3  Module system                   module.rs / installer.sh / hot_install.rs / module_config.rs / metamodule.rs / preinit.rs
│  ├─ U4  Images and installation         boot_patch.rs / lkm_image*.rs / late_load.rs / unload.rs
│  ├─ U5  Escalation and compatibility    su.rs / magica.rs / utils.rs
│  ├─ U6  System services                 sepolicy.rs / sulog.rs / resetprop.rs / soft_reboot.rs / restorecon.rs / profile.rs / feature*.rs / assets.rs / apk_sign.rs / debug.rs
│  └─ U7  Build and embedded assets       Cargo.toml / build.rs / bin/<arch>/
│
├─ M  Manager app (manager/)
│  ├─ M1  Skeleton and navigation         MainActivity / navigation3 / bottombar
│  ├─ M2  Home                            Home (status / info / update / jailbreak entry / reboot menu)
│  ├─ M3  Superuser and policy            SuperUser / AppProfile / Template / KsuService
│  ├─ M4  Module page                     Module (list / toggle / action / WebUI / shortcut / update)
│  ├─ M5  Module repository               data/registry + ModuleRepo*
│  ├─ M6  Install, flash, image analysis  Install / Flash / core/tasks(Payload/ExtractImage/BootKernelVersion)
│  ├─ M7  One-shot scripts                data/script + ScriptRun
│  ├─ M8  SU log                          Sulog
│  ├─ M9  Settings and theming            Settings / ColorPalette / theme / liquid
│  ├─ M10 Magica jailbreak                magica/ + cpp/adbroot.cc
│  ├─ M11 Native bridge                   Natives.kt + jni.cc + ksu.cc
│  ├─ M12 System integration              KsuCli / DownloadService / WebUI / shortcuts / icons
│  └─ M13 Packaging assets                Manifest / AIDL / protobuf / strings×46 / gradle
│
└─ P  Periphery and build
   ├─ P1 ksuinit (dual identity)          userspace/ksuinit
   ├─ P2 Docs site and READMEs            website/ + docs/
   ├─ P3 WebUI JS library                 js/
   ├─ P4 Build scripts                    repack_apk.py / scripts/ / justfile
   └─ P5 CI and build chain               .github/workflows/
```

## 2. Kernel driver (K)

### 2.1 Lifecycle and load forms

| ID | Feature | Implementation | Boot-critical | Existing switch | Removability |
| :--- | :--- | :--- | :--- | :--- | :--- |
| K1.1 | `kernelsu_init` assembly / ordered `kernelsu_exit` | `core/init.c:94`, `:207` | yes | — | core |
| K1.2 | LKM / built-in / late-load branches | `init.c:113`, `:154`, `:186` | yes | — | core |
| K1.3 | x86_64 `INDIRECT_SAFE` gate and dynamic dispatcher patching | `init.c:33,97`; `hook/x86_64/syscall_hook.c:209` | yes (x86) | `CONFIG_KSU_X86_PATCH_SYSCALL_DISPATCHER` | prunable per architecture |
| K1.4 | arm64 LKM self-built stack canary | `init.c:47,240` | yes | — | core |
| K1.5 | Module parameters `allow_shell` / `norc` / `bundled` | `init.c:79,86,89` | no | the parameters themselves | easy, **but stop writing bootconfig first** |

### 2.2 Hijack infrastructure (hook/ plus part of infra/)

| ID | Feature | Implementation | Notes | Removability |
| :--- | :--- | :--- | :--- | :--- |
| K2.1 | Syscall hook manager (tracepoint + kretprobe) | `hook/syscall_hook_manager.c:128,161` | registry for all syscall hooks | core |
| K2.2 | Unified dispatcher plus per-architecture routing table | `hook/{arm64,x86_64,riscv64}/syscall_hook.c` | forwards through one empty syscall slot | core |
| K2.3 | Syscall table install/restore | same, `:31-115` | installs the `read`/`fstat` hooks | core |
| K2.4 | Kernel text hot patching | `hook/*/patch_memory.c` (237/202/194 lines) | the basis of every patch capability | core |
| K2.5 | Syscall event bridge | `hook/syscall_event_bridge.c:53-145` | execve/execveat/newfstatat/faccessat/setresuid | core |
| K2.6 | setresuid hook | `hook/setuid_hook.c:24-56` | the only point where a uid switch is observed | core |
| K2.7 | Process tracepoint marking | `hook/tp_marker.c` (171) | scopes hooks to target processes | core |
| K2.8 | LSM slot hot replacement | `hook/lsm_hook.c` (475) | **its only consumer is selinux_hide** | removable with K5.5 |
| K2.9 | Symbol resolver | `infra/symbol_resolver.c` (391) | source of every patched symbol | core |
| K2.10 | fd wrapping layer | `infra/file_wrapper.c` (593) | stdio compatibility for terminal-style su | optional |
| K2.11 | su mount namespace | `infra/su_mount_ns.c` (156) | the root profile's namespace field | optional |
| K2.12 | seccomp bitmap cache rewriting | `infra/seccomp_cache.c` (65) | lets through seccomp-blocked ioctls | optional |
| K2.13 | Single-reader event queue | `infra/event_queue.c` (407) | basis of sulog | removable with sulog |

### 2.3 Kernel ABI: supercall (25 ioctls)

`[ksu_driver]` anon inode plus a reboot kprobe that hands out the fd (`supercall/supercall.c:31,116`); four permission tiers — `always_allow` / `only_manager` / `only_root` / `manager_or_root` (`supercall/perm.c`).

| ioctl | Purpose | Permission | Used by | Removability |
| :--- | :--- | :--- | :--- | :--- |
| `GRANT_ROOT` | escalate per profile | allowed_for_su | ksud su / Manager | core |
| `GET_INFO` / `GET_INFO_LEGACY` | version, flags, feature max, UAPI version | always_allow | everything | core (legacy can stay as a stub) |
| `REPORT_EVENT` | post-fs-data / boot-completed / module-mounted / services | only_root | ksud | core |
| `SET_SEPOLICY` | module and profile SELinux rules | only_root | ksud | core |
| `CHECK_SAFEMODE` | safe-mode boolean | always_allow | ksud/Manager | core (brick rescue) |
| `GET/NEW_GET_ALLOW_LIST`, `GET/NEW_GET_DENY_LIST` | allowlist reads (old and new, numbers reused) | manager_or_root | Manager | new one core, old one detachable |
| `UID_GRANTED_ROOT` / `UID_SHOULD_UMOUNT` | single-uid queries | manager_or_root | ksud/Manager | optional |
| `GET_MANAGER_APPID` | Manager uid | manager_or_root | Manager | core (Manager) |
| `GET/SET_APP_PROFILE` | per-app policy | only_manager | Manager | optional (`DISABLE_POLICY` exists) |
| `GET/SET_FEATURE` | 6 feature toggles | manager_or_root | Manager/ksud | core base |
| `GET_WRAPPER_FD` / `DISABLE_ESCAPE_TO_ROOT` | in-session su capabilities | manager_or_root + `allow_su_session` | ksud su | optional |
| `MANAGE_MARK` | process marking for debug | manager_or_root | ksud debug | optional |
| `NUKE_EXT4_SYSFS` | clear ext4 sysfs leftovers | manager_or_root | ksud | optional (removal drops the EXT4 dependency) |
| `ADD_TRY_UMOUNT` | umount list (wipe/add/del) | manager_or_root | ksud | optional |
| `SET_INIT_PGRP` | attach to the init process group | only_root | ksud | optional (ksud relies on it) |
| `GET_SULOG_FD` | audit event outlet | only_root | sulogd | removable with sulog |
| `LOOKUP_SYMBOLS` | batch kernel symbol lookup (≤1024) | only_root | ksud insmod/preinit | optional (ksud relies on it) |

**ABI iron rule**: `KERNEL_SU_UAPI_VERSION = 6` (`uapi/supercall.h:14`); `KSU_FEATURE_MAX = 7` with **id 5 permanently unused** (`uapi/feature.h:10`). Pruning unregisters a handler (returning `-ENOTTY` / `unsupported`) and **never** reuses a command number or feature id, because old `.feature_config` files and old Managers would resolve to a different function.

### 2.4 Policy and data (policy/)

| ID | Feature | Implementation | Removability |
| :--- | :--- | :--- | :--- |
| K4.1 | allowlist (RCU hash + kref, **not a bitmap**) | `policy/allowlist.c:60-70` | core |
| K4.2 | Persistence `/data/adb/ksu/.allowlist` (MAGIC/version 4, v2→v4 migration) | `allowlist.c:29,420,490` | core |
| K4.3 | Default root / non-root profiles (`$` keys) | `allowlist.c:41,245` | core |
| K4.4 | Full app_profile (uid/gid/groups/caps/domain/rules/namespace/flags/umount) | `uapi/app_profile.h`; `policy/app_profile.c` | optional (`CONFIG_KSU_DISABLE_POLICY`) |
| K4.5 | feature framework plus the `force_feature` parameter | `policy/feature.c:14,48,279` | core base |

### 2.5 Optional feature modules (feature/)

| ID | Feature | Lines | Default | What goes with it |
| :--- | :--- | :--- | :--- | :--- |
| K5.1 | `sucompat` (/system/bin/su → ksud) | 273 | on | **also delete the scoped su-session fd ioctls**, or a bypass with no entry point remains |
| K5.2 | `kernel_umount` (umount modules on uid switch) | 138 | on | non-root apps can see module mounts |
| K5.3 | `sulog` (event queue + fd + userspace sulogd) | 431 + 801 | off | also `infra/event_queue.c`, `GET_SULOG_FD`, the Manager Sulog page |
| K5.4 | `adb_root` (inject into adbd for root) | 233 | off | the userspace magica adb path dies with it |
| K5.5 | `selinux_hide` (fake SELinux view for apps) | 1257 | off | **also lsm_hook (475) and backup_sepolicy**: the largest single kernel block |
| K5.6 | `shell_su_adb_only` (shell escalates only via adbd) | 88 | off | none |
| K5.7 | `module_blacklist` (`block_modules=` parameter) | 96 | none | dropping the parameter makes machines with it in bootconfig **fail to load** |

### 2.6 Remaining kernel subsystems (K6–K10)

| Subsystem | Points | Size | Prunability |
| :--- | :--- | :--- | :--- |
| K6 SELinux | ksu domain rules (`selinux/rules.c:45`), 9 classes of dynamic policy patching (`rules.c:433` + `sepolicy.c`), setenforce, sid cache | 1752 | core |
| K7 Manager validation | APK V2 signature check (`manager/apk_sign.c`), throne scan and naming, fsnotify observation | 856 | `CONFIG_KSU_DISABLE_MANAGER` |
| K8 Boot injection | `KERNEL_SU_RC` plus init.rc/modules.rc read proxy (`runtime/ksud_integration.c:188-478`), ksud execve detection, volume-key safe mode, boot events | 755 | **core**: without it modules have no start point |
| K9 preinit | kernel-side respawn (`preinit/preinit.c`) plus the embedded static runner (`runner.c`, 987 lines; can `insmod`/`exec`/`write`) | 1162 | optional, via `preinit=0` or `-DKSU_NO_PREINIT` |
| K10 Build-time | `KSU` / `KSU_DEBUG` / `KSU_DISABLE_MANAGER` / `KSU_DISABLE_POLICY` / `KSU_X86_PATCH_SYSCALL_DISPATCHER`; signing constants; version = 30000 + git rev-list | — | the official trim surface |

## 3. Userspace daemon ksud (U)

### 3.1 CLI subcommands → subsystems (`cli.rs:25-156`)

| Subcommand | Subsystem | Called by | Wholly removable |
| :--- | :--- | :--- | :--- |
| `module install/uninstall/enable/disable/undo-uninstall/action/list/config` | U3 | Manager, installer.sh | no |
| `module preinit status/reset` | U3-preinit | users | with K9 |
| `post-fs-data` / `services` / `boot-completed` | U2 | injected init.rc | no |
| `late-load` (with `--magica`, `--post-magica`) | U4/U5 | Manager ZygotePreload, users | yes (with U5-magica) |
| `soft-reboot` | U6 | Manager | yes |
| `insmod` / `unload` | U4 | debug / LKM unload | yes |
| `install` / `uninstall` | U5 | Manager startup, uninstall flow | no (`uninstall` includes boot restore) |
| `boot-patch` / `boot-restore` / `boot-info *` | U4 | Manager install/flash/uninstall | partially (see §5.2) |
| `boot-patch-v2` | U4-lkm | **host/CLI only; the Manager never calls it** | yes (4183 lines, whole block) |
| `sepolicy patch/apply/check` | U6 | installer.sh, modules | no |
| `profile *` | U6 | Manager templates and policy | yes (if custom AppProfile goes) |
| `feature get/set/list/check/load/save` | U6 | Manager settings, installer.sh | partial |
| `kernel umount/nuke-ext4-sysfs/notify-module-mounted` | U4/U6 | internal | partial |
| `resetprop` | U6 | module scripts, system.prop | no (module ecosystem depends on it) |
| `initrc refresh` | U3 | users | with initrc |
| `debug *` (set-manager/get-sign/su/version/test/extract-binary/mark/sulogd/info/package) | U6 | developers | yes (most) |
| `sulogd` (hidden) | U6 | feature hook | with sulog |

### 3.2 Boot pipeline (`init_event.rs:11-125`)

```text
post-fs-data
 1  UAPI version gate → report_post_fs_data
 2  clear temp module config
 3  bootlog capture (logcat + dmesg, 30s)
 4  Magisk coexistence check (exit the whole stage on hit)
 5  safe-mode detection (kernel volume-key signal)
 6  generic scripts post-fs-data.d/
 7  extract embedded binaries to /data/adb/ksu/bin
 8  hot-install garbage collection; in safe mode disable all modules and exit
 9  move modules_update → modules, prune remove markers
10  rebuild modules.rc (initrc injection) and the preinit directory
11  restorecon → module sepolicy.rule → profile policy
12  forced features → push feature config
13  metamodule post-fs-data.sh → regular modules' post-fs-data.sh
14  system.prop (resetprop)
15  metamodule metamount.sh (OverlayFS mount) → post-mount stage
services        : report_services (idempotent) → service.d → metamodule → module service.sh
boot-completed  : report_boot_complete → preinit boot-count settlement → boot-completed.d → metamodule → modules
```

### 3.3 Subsystem detail

| ID | Subsystem | Main points | Size | Removability |
| :--- | :--- | :--- | :--- | :--- |
| U3.1 | Module install/uninstall/enable/list | zip unpacking, customize.sh environment, module.prop, icons, JSON listing, update, prune | `module.rs` 1201 | core |
| U3.2 | installer.sh | `ui_print`/`abort`/`set_perm`, REMOVE/REPLACE, partition moves, legacy compatibility, hot-install variables, managed-feature warnings | 493 | core (contains two **dead** functions: `mark_replace`, `set_permissions`) |
| U3.3 | Hot install | staged→live swap, immediate sepolicy load, optional scripts | `hot_install.rs` 219 | optional |
| U3.4 | Module config KV | binary KSUM v1, temp/persist, `manage.*` managed features | `module_config.rs` 472 | optional (feature/sulog depend on it) |
| U3.5 | metamodule | three hooks (metainstall/metamount/metauninstall), single instance, `/data/adb/metamodule` symlink | `metamodule.rs` 288 | optional (10 call sites need rewiring) |
| U3.6 | preinit sync | /metadata quota, manifest, bootcount circuit breaker | `preinit.rs` 553 | optional (shares a function with initrc; needs splitting) |
| U4.1 | boot-patch | unpack / inject LKM+ksuinit / repack / flash, KMI probing, A/B slots, backup, ksu_config | `boot_patch.rs` 1231 | keep the probing half |
| U4.2 | boot-patch-v2 | kallsyms recovery, GKI ABI, capsule, bootstrap relocation | `lkm_image.rs` 3301 + `btf` 680 + `.S` 202 | **wholly removable** |
| U4.3 | late-load | runtime ko load plus replaying all boot stages (jailbreak / no-flash) | `late_load.rs` 151 | optional |
| U5.1 | su | option parsing, identity/caps/secontext, global mnt, fd wrapping, `--ksu-no-new-privs`, ksurc | `su.rs` 366 | core |
| U5.2 | magica | adb port, rewriting ro.debuggable/ro.adb.secure, restoring properties, restarting adbd | `magica.rs` 172 | **easy** |
| U6.1 | sepolicy | 9 statement classes parsed + batch push + check | 779 | core |
| U6.2 | sulogd | events → daily rotated logs, retention policy | 801 | optional |
| U6.3 | resetprop | Magisk-compatible property tool + system.prop | 282 | core |
| U6.4 | soft-reboot | waitsys protocol + three-stage replay | 222 | optional |
| U6.5 | restorecon | xattr plus unlabeled scan | 65 | core |
| U6.6 | profile | per-app sepolicy plus template CRUD | 79 | optional |
| U6.7 | feature | config read/write, forced/managed decisions, push | 470+84 | core base |
| U6.8 | assets | embedded busybox/bootctl/waitsys/`*.ko` extraction | 79 | tied to features |

## 4. Manager app (M)

### 4.1 Screens and routes (`Routes.kt`, 15 entries)

| Screen | Core function | Main dependency | Removability |
| :--- | :--- | :--- | :--- |
| Home | status card, jailbreak entry, update card, system info, reboot menu (7 items) | Natives, MagicaService | core (jailbreak/update prunable) |
| SuperUser | grouped app list, multi-user enumeration, search/filter/sort, recently installed | KsuService AIDL, getAppProfile | core |
| Module | list, toggles, action, WebUI, shortcuts, update check, install FAB | `ksud module *` | core |
| Settings | 18 toggles and entries | three feature ioctls + preferences | core |
| AppProfile / TemplateEditor / AppProfileTemplate | tri-state policy, template CRUD, import/export | `ksud profile *` | wholly removable |
| ModuleRepo / ModuleRepoDetail | repository index, three detail tabs, integrity check, download | `modules.kernelsu.org` | **wholly removable** |
| Install / Flash | four install methods, partitions/KMI/LKM, flashing station, stock restore, permanent uninstall | `ksud boot-patch`/`boot-restore`/`uninstall` | partial |
| ExecuteModuleAction | action output console | `ksud module action` | foldable into Module |
| ScriptRun | one-shot script parameter form and runner | root shell | **wholly removable** |
| Sulog | log browsing/filtering/clearing | `ksud feature set sulog` | wholly removable |
| About / ColorPalette | version info / theming | — | removable |

### 4.2 Domain subsystems

| ID | Subsystem | Points | Size | Removability |
| :--- | :--- | :--- | :--- | :--- |
| M5 | Module repository | RegistryClient/Parser/Urls/UpdatePolicy/Integrity plus 5 UI files | ~4.1k lines + 21 strings | **first tier** |
| M7 | One-shot scripts | ScriptPackage/Params/Command/Repository/Record plus the runner screen | ~1.8k lines + 36 strings | first tier |
| M6 | Image analysis | `Payload.kt` (CrAU + protobuf + BZ/XZ + SHA-256), `ExtractImage.kt`, `BootKernelVersion.kt`, `DataSourceChannel` | ~863 lines + protobuf/commons-compress/xz | removable (with download-and-patch) |
| M10 | Magica | AppZygotePreload, MagicaService (isolated + useAppZygote), BootCompletedReceiver, `adbroot.cc`, JNI `forkDontCareAndExecKsud` | 77 Java + 123 cpp | removable as a group |
| M11 | Native bridge | 21 JNI entry points, 9 ioctl wrappers, Profile field mapping, `isFullFeatured` | 877 cpp | core |
| M12 | System integration | KsuCli (20+ commands), DownloadService, WebUI (1678 lines), shortcuts, icons/models | ~2.7k | partially prunable |
| M9 | Theming | 7 colour modes, Monet, 15 keyColors, PaletteStyle, blur, floating bottom bar, liquid glass, badges, scaling, gestures | ~3k (liquid: 430 lines of dead references) | wholly removable |
| M13 | Packaging assets | Manifest components, AIDL, protobuf, 375 strings × 46 locales, 24 gradle dependencies | — | tied to features |

## 5. Key coupling (read before pruning)

1. **init.rc injection is the module system's only start point** (K8) — removing it removes post-fs-data/services/boot-completed at once.
2. **Module parameters are an invisible hard dependency**: if `allow_shell`/`norc`/`bundled`/`block_modules`/`preinit`/`force_feature` disappears while bootconfig still carries the key, **module loading fails**. Stop `ksud` writing those keys (`boot_patch.rs`'s ksu_config) before deleting the kernel side.
3. **feature ids and ioctl numbers are append-only** (K3 iron rule); `KSU_FEATURE_MAX` stays 7.
4. **sucompat ↔ scoped su-session fd ↔ `--ksu-no-new-privs`** move together.
5. **selinux_hide ↔ lsm_hook ↔ backup_sepolicy** move together.
6. **boot-patch cannot go wholesale**: the Manager's direct install / download-and-patch / OTA slot switching, the five `boot-info` subcommands, `ksud uninstall`'s boot restore, and `late_load`'s KMI probing all depend on it. Lift KMI/slot/partition probing into a shared module, then delete only patch and restore.
7. **lkm_image is wholly removable** (dependencies point one way), but the bootstrap assembly in `build.rs` goes too, or the build still demands clang/aarch64-gcc.
8. **Removing metamodule touches 10 call sites** (init_event ×3, late_load, module.rs ×4, hot_install), and devices with meta-overlayfs installed degrade to "modules not mounted".
9. **preinit and initrc are emitted by one function** (`module.rs:620`); keeping initrc while dropping preinit requires splitting it.
10. **Embedded assets**: busybox is not removable; bootctl goes with A/B OTA finalisation; waitsys with soft-reboot; `*.ko` needs all three consumers gone and a matching `.github/workflows/ksud.yml` change.
11. **Two Manager UIs**: every screen has a Material and a Miuix implementation; dropping one saves roughly 40% of UI code and five dependencies.
12. **Fact checks**: the Manager calls only `boot-patch`/`boot-restore -f`/`uninstall`/`soft-reboot` — **never boot-patch-v2**; gradle has **no bouncycastle** (SHA-256 comes from the JDK).

## 6. Pruning decision matrix (suggested)

| Goal | Suggested order | Estimated reduction |
| :--- | :--- | :--- |
| Less code, low risk | module repository → one-shot scripts → theming (including collapsing the double UI) → Sulog → templates → shortcuts → boot-patch-v2 (lkm_image, 4183 lines) → debug/sulogd/insmod/unload | Manager ~12k lines, ksud ~5k lines |
| Smaller escalation surface | preinit runner (can `insmod`) → adb_root → sucompat → Magica → selinux_hide | kernel ~1.5k lines, ksud 172, Manager 200 |
| Smaller kernel | selinux_hide (+lsm_hook) → sulog (+event_queue) → file_wrapper → pkg_observer → nuke_ext4_sysfs (drops the EXT4_FS dependency) → non-arm64 architectures | kernel ~3k lines |
| Less maintenance | metamodule → preinit → unneeded locales → website/js | docs and CI shrink sharply |
| **Do not touch** | init.rc injection, supercall base, feature framework, allowlist/profile, SELinux dynamic patching, boot-patch probing, safe mode | — |

**Residue checklist for every step**: Routes + MainActivity entry + ViewModel + Repository + strings (46 locales) + Manifest components + gradle dependencies + JNI/ksud commands + `doc/` and website documents + CI workflow.

## 7. Fork decisions still open

1. Keep the **download-and-patch boot** line in the Manager? (decides M6 and protobuf/commons-compress/xz)
2. Keep the **metamodule architecture**? (decides whether modules can still modify `/system`)
3. Keep **per-app policy** (AppProfile/templates)? (decides K4.4, U6.6, and Manager group D)
4. Collapse **Miuix or Material** into a single UI?
5. Abandon **LKM/late-load** entirely (ksuinit, Magica, boot-patch-v2)?
