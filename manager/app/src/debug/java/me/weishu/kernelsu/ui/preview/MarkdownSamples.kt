package me.weishu.kernelsu.ui.preview

/**
 * Markdown samples for the style previews.
 *
 * Test data, so it lives beside the previews that render it rather than in the app: nothing at
 * runtime reads these strings, and shipping them in `main` would put several KB of sample
 * changelog into the APK.
 *
 * Three documents rather than one, because they answer different questions:
 *
 * - [MARKDOWN_COMPAT_INLINE] covers every inline and list construct, including the ones the
 *   renderer handles badly (unbreakable URLs, unbreakable tokens, mixed scripts).
 * - [MARKDOWN_COMPAT_BLOCK] covers every block construct: quotes, tables, alerts, images.
 *   Splitting the compatibility corpus is forced by a cap in the screenshot harness: a
 *   render taller than 3000dp is truncated, so one document cannot hold both halves.
 * - [MARKDOWN_SHOWCASE] is a module changelog shaped like the real ones; it is what the style
 *   is tuned for.
 *
 * Changelogs rarely use most of the compatibility constructs, but a module can publish any
 * markdown, so each one is still rendered and checked.
 */

/** Inline and list constructs. */
const val MARKDOWN_COMPAT_INLINE = """
# Heading level 1

## Heading level 2

### Heading level 3

#### Heading level 4

##### Heading level 5

###### Heading level 6

Setext heading level 1
======================

Setext heading level 2
----------------------

A paragraph with **bold**, *italic*, ***bold italic***, ~~strikethrough~~, `inline code`, a
[link with text](https://kernelsu.org), an autolink <https://github.com>, and a reference
[link defined later][ref].

A second paragraph directly follows, so paragraph spacing is visible. It also carries a very
long unbreakable URL to prove wrapping behaviour:
https://example.com/a/really/long/path/that/cannot/be/broken/anywhere/and/keeps/going/on/forever

And a long unbreakable token: SupercalifragilisticexpialidociousPneumonoultramicroscopicsilicovolcanoconiosis

Mixed scripts: module changelog / 中文与 Latin 混排 / emoji

Escapes: a literal asterisk and a literal backtick, then a bare ampersand: &

---

## Lists

- First level
- First level with children
  - Second level
    - Third level
      - Fourth level, deepest this sample goes
- Back to first level

1. Ordered item
2. Ordered item
   1. Nested ordered
   2. Nested ordered
3. Ordered item

- Mixed parent
  1. Ordered child
  2. Ordered child
- Mixed parent

## Task list

- [x] Completed task
- [x] Another completed task
- [ ] Pending task
- [ ] Pending task with `code` inside

## Code

Inline code with a keyword: `execKsud` and a path: `/data/adb/modules`.

Fenced with a language:

```bash
su -c "ksud module install /sdcard/mod.zip"
ksud module list | grep enabled
```

Fenced without a language:

```
plain fenced block
no language tag
```

Indented code block:

    indented code block
    four spaces into the line

[ref]: https://kernelsu.org "KernelSU"
"""

/** Block constructs: quotes, tables, alerts, images. */
const val MARKDOWN_COMPAT_BLOCK = """
# Block constructs

## Quotes

> A single-level quote with **bold** and a [link](https://kernelsu.org).
>
> It continues across two paragraphs.

> Outer quote
> > Nested quote
> > > Deeply nested quote

## Table

| Element | Supported | Notes |1|2|3|
| :--- | :---: | :--- |:---|:---:|---:|
| Headings | Yes | Six levels plus setext |1|1|1|
| Tables | Yes | Horizontal scroll when wide |1|1|1|
| Alerts | Yes | Five GitHub types |1|1|1|

A narrow table:

| A | B | C |
| :--- | :---: | ---: |
| left | centre | right |

A table with a long cell, which wraps instead of being cut off:

| Key | Value |
| :--- | :--- |
| kernel.su.compat | Whether the running kernel exposes the supercall interface |

## Alerts

> [!NOTE]
> Useful information a reader should know.

> [!TIP]
> A shortcut that makes something faster.

> [!IMPORTANT]
> Something required to get the right result.

> [!WARNING]
> Something that may cause a problem.

> [!CAUTION]
> Something that may cause damage.

## Image

![Placeholder](https://example.com/image.png)
"""

/**
 * A module changelog shaped like the real ones.
 *
 * Built from a survey of published module changelogs: an optional preamble of links, then one
 * heading per release, newest first, at whichever level the author chose - so this one mixes
 * H2 and H3 on purpose. Bullets carry nearly all content; inline code, bold and links appear
 * inside them; long items must wrap and hang. A four-column table is wider than a phone's text
 * column, so it shows the table scrolling past the inset. It ends with reference-link
 * definitions, which some changelogs accumulate by the hundred and which must not render as
 * blank space.
 */
const val MARKDOWN_SHOWCASE = """
Telegram: https://t.me/example_module · [GitHub](https://github.com/example/module)

## v2.4.0 🎉
- 支持 **KernelSU** 的 metamodule 挂载方式，旧版 Magisk 仍按原逻辑处理
- Fix `service.sh` exiting early when `/data/adb/modules/example/disable` exists after a soft reboot, which left the hosts file unmounted until the next full boot ([#128])
- Reworked the WebUI
  - Settings are saved immediately, no more "Apply" button
  - Dark theme follows the system
- [ ] Known issue: the quick tile is not updated on Android 16 QPR1

> Upgrading from v2.3.x keeps your configuration. Downgrading does not.

### v2.3.1
* Fixed a crash on boot on some MIUI / HyperOS builds ([#121])
* 更新 `busybox` 至 1.36.1

| 设备 | Android | 状态 | 备注 |
| :--- | :---: | :---: | ---: |
| Pixel 8 | 16 | ✅ | — |
| Xiaomi 14 | 15 | ⚠️ | 需关闭 MIUI 优化 |

### v2.3.0
1. New `action.sh`: tap **Action** in the manager to refresh the list
2. Dropped support for Android 9 and below

---

#### Notes
Reinstall the module if `ksud module list` reports it as *broken*.

[#121]: https://github.com/example/module/issues/121
[#128]: https://github.com/example/module/pull/128
"""
