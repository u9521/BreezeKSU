# 2026-11-02 — 文档站部署到 GitHub Pages 项目子路径

## 问题

文档站此前从未真正上线：`https://u9521.github.io/BreezeKSU/` 返回 GitHub 的 "Site not found"，`deploy-website.yml` 的工作流运行次数为 0。

兄弟仓库 `u9521/KernelSU` 则上线了但页面空白，控制台里九条 404：

```
GET https://u9521.github.io/assets/app.Dk8cOxPW.js          404
GET https://u9521.github.io/assets/style.DFTx90Kk.css       404
GET https://u9521.github.io/vp-icons.css                    404
GET https://u9521.github.io/logo.png                        404
```

两类 URL 都指向 `https://u9521.github.io/` 这个**站点根**，而产物实际发布在 `/KernelSU/` 子路径下。实测同一批文件在 `https://u9521.github.io/KernelSU/assets/app.Dk8cOxPW.js` 返回 200。根因是 VitePress 配置里没有 `base`，HTML 里写的是绝对路径 `/assets/...`。

顺带发现两件与部署耦合的事：站点 `head` 里挂着**上游的** AdSense 脚本（`ca-pub-2610070972052494`），文档正文也还是上游原文。

## 决定

- **`base: '/BreezeKSU/'` 写死在 `website/docs/.vitepress/config.ts`**，不从 CI 注入。VitePress 只自动补尾部斜杠，不补首部（源码里是 `base.replace(/([^/])$/, '$1/')`），所以值必须首尾都带斜杠，写成 `'BreezeKSU/'` 会静默出错。
- **站内绝对路径一律不手改**：实测 8 个语种 `index.md` 的 `src: /logo.png` 和 `link: /guide/...` 分别走 `VPImage`/`VPButton` → `withBase()`，markdown 里以 `/` 开头的链接由 `linkPlugin` 拼前缀，构建产物自动带 `/BreezeKSU/`。全仓 `](/...)` 形式一处都没有。
- **删广告**：`config.ts` 的 `head` 块与 `public/ads.txt`。用一个不属于本仓库的 AdSense 账号在自己域名下拉广告，既没有收益也违反 AdSense 条款。
- **删 `robots.txt` 的 Sitemap 指令**：指向 `https://kernelsu.org/sitemap.xml`，站点上线后等于主动把爬虫送去上游。
- **WIP 横幅用主题插槽，不用 `head` 注入**：新建 `.vitepress/theme/index.ts` `extends` 默认主题，在 `layout-top` 插槽渲染 `WipBanner.vue`。横幅实测宽度后写 `--vp-layout-top-height`，这是 VitePress 默认主题为这个用途预留的变量，`VPNav` 的 `top`、`VPSidebar` 的 `top`、`VPLocalNav` 的 `padding-top` 都消费它。
- **只留英文和简中**：删 `zh_TW`/`ja_JP`/`vi_VN`/`id_ID`/`ru_RU`/`pt_BR` 六个目录及其 `locales/*.ts`。这六个语种全部是继承来的上游译文，fork 从未维护过自己的内容。

## 被否决的选项

- **自定义域名**（如 `breeze.example.com`）：自定义域名服务在根路径，`base` 要保持 `'/' `；等于把「部署」和「域名采购 + DNS + 证书」两件事绑在一起，而现在连构建产物都还没验过。
- **`gh-pages` 分支发布**：现有工作流走的是 GitHub Actions artifact（`upload-pages-artifact` → `deploy-pages`），不是分支推送。换成分支发布要多维护一个孤儿分支和一次 force-push。
- **让 `actions/configure-pages@v6` 自动处理 base**：查过 v6 的源码，它只支持 nuxt/next/gatsby/sveltekit 四种 SSG，VitePress 会走到 `throw Unsupported static site generator`，且只导出 `GITHUB_PAGES` 与 `base_url`/`base_path` 等 output，从不写配置文件。手动 `base` 无法省略。
- **从 `steps.pages.outputs.base_path` 注入 `BASE_PATH`**：本地构建默认 `'/'`，与 CI 产物不一致，而本地预览恰是最容易发现 base 写错的场合。
- **把横幅塞进 `head`**：`head` 里的裸 URL 不被 `withBase()` 处理，且注入的 HTML 只在首次加载渲染，SPA 路由切换时行为不可靠。
- **整站品牌化**（`SITE_URL`、`sitemap.hostname`、148 处 `tiann/KernelSU` 链接、10 处 `t.me/KernelSU`）：改动面是 129 个 md 文件，混进来会让「站点能打开」这个验收标准变成「站点内容全对」，永远验不完。

## 后果

- `base` 与仓库名绑定：仓库改名必须同改 `config.ts`，否则整站 404。已在配置里注释说明。
- VitePress 默认主题没有内置 banner，这是 fork 第一次自定义主题。上游今后改主题结构时，`layout-top` 插槽和 `--vp-layout-top-height` 是唯二依赖，改动要做好这两处会变的准备。
- 删掉六个语种后，`llms.txt` 的 `translatedDocs` 会自动缩小（它从 `locales.locales` 派生），无需另改。
- `.nojekyll` 在这个发布方式下是死代码：Jekyll 处理只对分支发布生效；而 `upload-pages-artifact@v5` 默认 `include-hidden-files: false`，tar 时带 `--exclude=.[^/]*`，工作流里 `touch` 出来的那个文件根本进不了产物。

## 已知偏差（本轮不处理）

站点仍在上游的元数据下发声，四处：

| 位置 | 现值 | 影响 |
| :--- | :--- | :--- |
| `website/docs/.vitepress/seo.ts` | `SITE_URL = 'https://kernelsu.org'` | JSON-LD 的 `url`/`image` 与 `llms.txt` 的 `domain` 指向上游 |
| `website/docs/.vitepress/config.ts` | `sitemap.hostname = 'https://kernelsu.org'` | 站点地图里的绝对 URL 指向上游 |
| `website/docs/**/*.ts,md` | 148 处 `https://github.com/tiann/KernelSU` | 导航、页脚、编辑链接、侧边栏仓库链接 |
| `website/docs/**/*.md` | 10 处 `https://t.me/KernelSU` | 社区入口 |

搜索引擎会把本站当成 `kernelsu.org` 的副本。这四处与品牌化一起做才有意义，单独改任何一处都只会在同一个问题上留下半截状态。

另有一条与广告无关但同样属于「指向别人」的：`config.ts` 的 `llmsTxtTemplate` 里写死了 `Source code: https://github.com/tiann/KernelSU` 和 `Downloads: https://github.com/tiann/KernelSU/releases`。
