# Upstream change classes

| Class | Signal | Default action |
| :--- | :--- | :--- |
| Security fix | CVE / bounds check / out-of-bounds / UAF / privilege escalation | **must port** |
| Crash or corruption | "fix crash" / "avoid panic" / null deref / race | **must port** |
| Kernel compatibility | diff contains `KERNEL_VERSION` / new KMI / `linux -` | **must port** (kernel-compat lane) |
| UAPI / ABI | `uapi/` change / `KSU_FEATURE_MAX` / ioctl number | **human decision**, three layers ship together |
| New feature | `feat:` / new directory / new ioctl / a strings batch | skip; record in the skipped list |
| Refactor | pure move or rename, no behaviour change | skip; re-evaluate if it moves code out of a pruned area |
| Docs / CI / translations | `website/`, `.github/`, `values-*` | skip |

## Three lanes, different rhythms

| Lane | Content | Cadence |
| :--- | :--- | :--- |
| kernel-compat | only patches carrying `KERNEL_VERSION` | every 3 months, or within 1 month of a new KMI |
| uapi | locked; moves only when a human decides to bump | event-driven |
| ui | drift between `ui/` and the upstream contract | whenever upstream changes UI |

## Why UAPI is always a human decision

Upstream moved 6 UAPI versions in 4.5 months, with a peak of one breaking change every 3–6 weeks. All three layers are hard-coupled:

- `ksucalls.rs`'s `ensure_uapi_version_matched()` bails when versions differ.
- The app's `isFullFeatured()` requires exact equality with the kernel, otherwise the whole UI degrades.
- Legacy ioctls backfill `uapi_version = 0`, which never matches.

So a UAPI change must land kernel, daemon and app in one commit — never just one of them.

## Skipped list

Every sync writes `doc/sync/<date>/skipped.md`: commit sha, title, class, reason for skipping, whether it needs re-evaluation. This is the only record that a skip was deliberate, and it stops the same commit being re-argued months later.
