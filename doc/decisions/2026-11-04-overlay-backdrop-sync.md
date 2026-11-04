# 2026-11-04 — overlay 背景从配置类改成两个接缝

## 问题

模板（`breezexample`）在 2026-11-04 连改四次 overlay，尾部要跟进：

1. `OverlayAppearance` 把三件互不相关的事（画什么背景、遮罩多强、时间轴多长）塞进一个数据类，想换背景只能加字段。上游删掉它，换成两个正交接缝。这条取代 [2026-11-02 记录](2026-11-02-manager-build-migration.md) 里「`BreezeOverlayHost(backdropBlur = …)` 是整叠层默认值，`OverlayAppearance.blur`（可空）按层覆盖」那一条：两个类型都不存在了。
2. `BreezeOverlaySheet` 默认 `backdrop = OverlayBackdrop.sheetBlur()`，非 `null` 因而压过宿主。用户关掉模糊后，dialog 变成纯遮罩而 sheet 仍然模糊。
3. sheet 的模糊强度与拖动位移线性对应，而卡片本身走 Material emphasized 曲线；dialog 的卡片走 `(0.2, 0, 0, 1)` 而背景走 `tween` 默认的 `FastOutSlowInEasing`。
4. 拖拽关闭时 `visible` 仍为 `true`，只置 `dismissing`，所以按 `visible` 门控的背景淡出从不触发。

## 决定

- **背景是一个实现槽，不是一堆开关。** `OverlayBackdrop` 的 `Backdrop(scope, content)` 拿到进度、`hazeState`、已解析的遮罩透明度、`isTopmost` 与外部点击回调，自己组织卡片后面画什么。`Default` / `sheetBlur()` / `Scrim` 是三个内建值，自定义就是再实现一个接口。
- **`samplesHost` 一个布尔值同时是声明与开关**：为真的背景要宿主录制的页面内容，为假的一律不录。叠层压暗由 `OverlayLayer` 算进 `scope.scrimAlpha`，自定义背景不用重新实现一遍。
- **进度与背景正交。** `OverlayProgress` 只回答 0..1 从哪来：`Fade(visible, …)` 自己播 `Animatable`，`External { value() }` 逐帧转发调用方。sheet 用后者接 `SheetState.requireOffset()`，于是拖动、`show()`、`hide()` 与锚点回弹共用同一个值。
- **曲线放在进度出口，不放在背景里。** 归一化行程在交给任何消费者之前先过 `OverlayEnterEasing`；模糊半径、遮罩透明度、自定义背景自动继承，没有第二处需要记得加。缓动常量与 220/150ms 集中在 `OverlayProgress.kt`，`BreezeAlertDialog` 的卡片引用同一组，卡片与背景因此只有一个时间轴。
- **两个组件的 `backdrop` 默认改回 `null`（跟随宿主）**，关掉全局模糊时 dialog 与 sheet 一起变成纯遮罩。想要恒模糊的调用方显式传 `sheetBlur()`。
- **退场卸载等两条动画都结束**：`hide()` 与 `OverlayExitMillis` 的淡出并跑，`coroutineScope` 等齐再拆层；否则快速下滑时模糊会在 sheet 消失后「啪」地掉。
- **镜像而不是重新表达。** `ui/component/breeze/` 是模板同路径树的镜像：代码与 KDoc 一起照抄，fork 只改 `package`/`import`。这批之前 fork 在这几个文件里的全部差异就是注释换行，重写注释是纯成本，而且上游 commit 正文就是那些注释的出处。镜像关系记在 [seams](../seams.md) Part 2。

## 被否决的选项

- **保留 `OverlayAppearance`，只把曲线改成统一 easing**：最小侵入，但会在 fork 里留一个上游已删的类型，`seams.md` 上多一条 fork 自有的修改，下次同步要手工判断保留哪半边。
- **只抄 `blur()` 工厂里被调用过的重载**：`sheetBlur()` / `blur(radius, scrimAlpha, …)` 在本仓与模板的调用点都是零——但删掉它们，下次上游改工厂就冲突，而「跟着模板走」正是这批的前提。
- **手写一个上游从未存在过的 `BreezeOverlaySheet.kt` 中间版本**（只含 `External`、不含缓动与退场修复），好让两条 commit 各自对应一个上游语义：那个版本既不在任何仓库里，也不能独立验证，下次同步时它是第三份要维护的文本。改成按文件切：第一条落接口层，第二条整份落 sheet。
- **给 `BreezeOverlaySheet` 的 `backdrop` 保留 `sheetBlur()` 默认值**：那是行为不一致的来源本身。

## 后果

- 删除 `overlay/OverlayAppearance.kt`，新增 `overlay/OverlayProgress.kt` 与 `overlay/OverlayBackdrop.kt`；`BreezeOverlayHost` 的参数从 `backdropBlur: OverlayBlur?` 变成 `backdrop: OverlayBackdrop?`，`BreezeShell` 的两个分支随之改成 `OverlayBackdrop.Default` / `OverlayBackdrop.Scrim`。
- `BreezeOverlay` 失去 `scrimVisible` 参数（仓内没有 screen 层调用方传它，两个传它的地方都在本批被覆盖的文件里）。
- 用户可见：关掉模糊后 sheet 与 dialog 表现一致；sheet 拖到一半时背景跟手；拖拽关闭时模糊跟着淡出；dialog 背景不再滞后于卡片。
- `overlay.md` 与 `overlay-stack.md` 的「`OverlayAppearance`」整节、以及「每层各自画模糊」两句一并改写——它们在同步后就是假话。
