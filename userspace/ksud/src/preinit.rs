//! Module preinit no longer exists in this fork.
//!
//! Upstream KernelSU stages a nolibc runner inside the kernel module and executes each enabled
//! module's `preinit/preinit.conf` before SELinux policy is loaded. BreezeKSU removed the kernel
//! side entirely, so nothing would ever run what `sync()` used to copy to `/metadata`.
//!
//! The functions below are kept, with their signatures unchanged, for two reasons: the call sites
//! in `module.rs`, `init_event.rs` and `cli.rs` stay as they are, and the `ksud module preinit`
//! subcommand keeps working instead of failing as an unknown command.

use anyhow::Result;

use std::{
    collections::{BTreeMap, HashMap},
    path::{Path, PathBuf},
};

/// No-op: nothing is staged onto `/metadata` because no kernel runner would consume it.
///
/// The `Result` stays in the signature even though this body cannot fail: every call site runs
/// `?` on it, and a signature that shrinks back to `()` would stop this being a drop-in stub.
#[allow(clippy::unnecessary_wraps)]
pub const fn sync(
    _ksu_dir: &Path,
    _stale_ksu_dirs: &[&Path],
    _modules: &BTreeMap<String, PathBuf>,
    _con: &str,
    _supported: bool,
) -> Result<()> {
    Ok(())
}

/// No-op: there is no boot counter to settle.
pub const fn on_boot_completed() {}

/// No-op: there is no boot counter to clear. Kept so `ksud module preinit reset` succeeds.
///
/// Same reason as [`sync`]: the call site runs `?`, so the `Result` is part of the interface.
#[allow(clippy::unnecessary_wraps)]
pub const fn reset_bootcount() -> Result<()> {
    Ok(())
}

/// No module ever reports a preinit state, so the caller sees an empty map.
pub fn module_states() -> HashMap<String, String> {
    HashMap::new()
}

/// No module ever reports a preinit state, so there is never one to describe.
pub const fn status_of(_id: &str) -> Option<(String, String)> {
    None
}

/// No-op: `ksud module preinit status` now prints nothing.
pub const fn print_status() {}
