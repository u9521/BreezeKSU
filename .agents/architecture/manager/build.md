# Manager build

Covers: manager/build.gradle.kts, manager/settings.gradle.kts, manager/gradle/, manager/app/build.gradle.kts, manager/app/src/main/cpp/, manager/keystore/

## Toolchain is pinned, and the wrapper fetches it

`gradle/gradle-daemon-jvm.properties` pins the daemon to Oracle JDK 23 and carries a `toolchainUrl.*` per platform, so Gradle downloads that JDK itself. Do not delete it to "fix" a local JDK problem; there is no toolchain resolver plugin, since the daemon-JVM download is a Gradle feature rather than a plugin.

The wrapper is Gradle 9.8.2 with `distributionSha256Sum` pinned. AGP 9.5.0 refuses to load below Gradle 9.6.0, so the two move together.

**The configuration cache is on.** No configuration-time file reads (`FileInputStream`, `readText`, `Runtime.exec`); go through `providers.fileContents(...)` or `providers.exec { }` so the read is a tracked input. The signing properties and the git version numbers already do. `discarded with N problems` means something slipped back.

Build through the Windows wrapper: `/mnt/c/Windows/System32/cmd.exe /c "gradlew.bat :app:assembleRelease"` from `manager/`. WSL's own `./gradlew` re-reads every source over the 9p mount. A stale daemon locking `fileHashes.lock` is cleared with `gradlew.bat --stop`.

`jniLibs` must already contain `libksud.so` for every ABI before any Gradle command — see [userspace](../userspace.md). `riscv64` has none checked in locally; CI supplies all three.

## Release is one APK per ABI

`splits { abi { include(three ABIs); isUniversalApk = false } }` emits one APK per architecture. There is no universal APK, and `defaultConfig.ndk.abiFilters` must stay empty: AGP rejects a build setting both.

Names follow `archivesName`: `BreezeKSU_<versionName>_<versionCode>[-debugkey]-<abi>-release.apk`.

A release download is the **repacked** APK from `dist/`, not the Gradle output: `repack_apk.py` injects `libksud.so`, strips it, zipaligns at 16 KB and re-signs v2-only. Pass `-i <apk>` per file and a name carrying the ABI.

## Signing is optional, and the fallback is named

Release signing reads `manager/keystore/sign.properties` (`keystoreFile` against `manager/`) and falls back to the debug key when the file or keystore is missing. Every value can be overridden by a same-named environment variable.

The fallback marks the artifact: `archivesName` gains `_debugkey`. The marker sits there rather than in `outputFileName` because a split variant has one output per ABI — a single `outputFileName` would collide across them, and reading it back to build the new name evaluates the property against itself.

CI keeps its own rule: a non-pull-request build with no `KEYSTORE` secret fails outright. The fallback exists so a fresh clone can build, not so a release ships under the debug key.

## R8 shrinks everything

`release { optimization { enable = true } }` turns on code shrinking, obfuscation and resource shrinking together; `isMinifyEnabled` / `isShrinkResources` are unused. `packageScope` stays at its default `**` — setting it makes the block a whitelist, and unlisted dependencies silently stop being optimised.

Keep rules live in `app/src/main/keepRules/rules.keep` (AGP merges every `*.keep`; `proguard-rules.pro` is gone). They pin names, not code: `-keepnames me.weishu.kernelsu.**` keeps stack traces readable while dead code still goes, and `@JavascriptInterface` members are kept explicitly because the WebUI calls them by name.

Previews build in the debug source set only (`app/src/debug/java/.../ui/preview/`). There is no screenshot-test plugin: behaviour is verified on a device, not by rendered snapshots.
