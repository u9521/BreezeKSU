# 2026-11-02 — Manager 构建迁移：分架构打包、可选签名、R8 DSL、overlay 模糊

## 问题

模板（`breezexample`）在 2026-11-01/02 连续改了四件事，尾部（BreezeKSU）要跟进：

1. release 只产出一个胖 APK，用户下载时把三个 ABI 的原生代码全带走，其中两个永远用不到。
2. 签名依赖 `lsplugin.apksign` 第三方插件，从 `gradle.properties` 读四个属性；缺签名时产物仍叫 `app-release.apk`，分不清能不能发布。
3. release 仍用 `isMinifyEnabled` + `proguardFiles` 的老写法，未启用配置缓存。
4. overlay 叠层只允许最顶层模糊，被覆盖的层会掉模糊。

## 决定

- **ABI splits 代替 abiFilters**：`splits { abi { include(三个 ABI); isUniversalApk = false } }`。`defaultConfig.ndk.abiFilters` 必须留空——AGP 同时设置两者会直接报 `Conflicting configuration`。不产 universal 包：那等于把刚省下的体积再发一份。
- **`-debugkey` 标记写进 `archivesName`**，不用 `output.outputFileName`。split 变体一个 variant 三个 output，同一个 `outputFileName` 会互相覆盖；而回读 `outputFileName` 拼新名字会触发 `Circular evaluation detected`。
- **签名换成模板那套**：`manager/keystore/sign.properties` + `providers.fileContents`，密码可被同名环境变量覆盖；缺 keystore 回退 debug key。删除 `lsplugin.apksign`、`manager/sign.example.properties`、`manager/key.jks` 的历史约定。
- **CI 的非 PR 构建缺 `KEYSTORE` 直接失败**：回退机制是给"克隆即构建"用的，不是给发布用的。
- **R8 走 `optimization { enable = true }`**，不写 `packageScope`（写它会把语义变成白名单，未列出的依赖静默不优化）。keep 规则搬到 `app/src/main/keepRules/rules.keep`，用 `-keepnames` 保名不保码，并显式保留 `@JavascriptInterface` 成员——WebUI 按名字调用它们。
- **版本号走 `providers.exec`**，配置阶段不再 `Runtime.exec`；`rev-list --count` 返回 1（浅克隆）时构建失败，而不是静默发出一个版本号回退的包。
- **截图测试删除，预览进 debug source set**（`app/src/debug/java/.../ui/preview/`）。预览需要 `@Preview` 注解，依赖 `ui-tooling-preview`，只对 debug 可见。
- **overlay 模糊配置上提到 host**：`BreezeOverlayHost(backdropBlur = …)` 是整叠层默认值，`OverlayAppearance.blur`（可空）按层覆盖；overlay 内部不再读 `LocalEnableBlur`。

## 被否决的选项

- **用 `repack_apk.py` 三次裁剪胖 APK**：构建期就要把三个 ABI 都编出来打进一个包，再裁掉两个，白付构建时间，且每裁一次就要重签一次。改成 Gradle split 后每个包只签一次。为了让脚本能按文件跑，补了 `-i/--apk`。
- **保留 `isMinifyEnabled`/`isShrinkResources`**：AGP 9.5 里它们与 `optimization.enable` 是 OR 关系，留着不会报错，但会失去"显式关掉"的能力，且两套写法并存让配置来源不可读。
- **`optimization.packageScope` 白名单**：实测只列常用命名空间时，未列出的第三方依赖完全不优化；要全优化就得穷举。
- **继续用 `lsplugin.apksign` 并补回退**：插件是第三方的，配置缓存兼容性不在我们手里；一套机制比两套好。
- **`outputFileName` 加 `-debugkey` 后缀**：与 ABI split 冲突，见上。

## 后果

- release 从 1 个包变成 3 个，每个只含自己的 ABI。本次实测：arm64-v8a 7.4 MB、x86_64 7.2 MB、riscv64 3.6 MB。
- 发布只发 arm64-v8a 和 x86_64；riscv64 编译并保留为 CI artifact，因为没有消费级设备。
- `gradle.properties` 里的 `android.experimental.enableNewResourceShrinker.preciseShrinking` 已删：AGP 9.0 起它是 Removed 状态，且 AGP 认的是不带 `experimental.` 的键名，原来那行是死配置。
- 配置缓存已开启并实测写入成功。今后任何配置期读文件都会让它静默失效，报 `discarded with N problems`。
- overlay 的堆叠模糊、`scrimAlphaStacked`、按层 `appearance` 覆盖都随模板落地；`overlay.md` 与 `overlay-stack.md` 同步改写了"只有最顶层模糊"的旧描述。
- 删除 foojay resolver 插件：daemon JVM 的下载走 `gradle-daemon-jvm.properties` 里的 `toolchainUrl.*`，与该插件无关。
