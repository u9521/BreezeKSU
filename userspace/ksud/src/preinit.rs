//! Module preinit no longer exists in this fork: the kernel side that ran the runner is gone,
//! so nothing would consume what `sync()` used to stage.
//!
//! The stubs stay, signatures unchanged, so the call sites and the `ksud module preinit`
//! subcommand keep working rather than failing as unknown.

use anyhow::Result;

use std::{
    collections::{BTreeMap, HashMap},
    path::{Path, PathBuf},
};

/// No-op. The `Result` stays in the signature although the body cannot fail: every call site
/// runs `?` on it, and shrinking it would stop this being a drop-in stub.
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
