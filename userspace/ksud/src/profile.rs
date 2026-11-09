use crate::defs;
use crate::utils::ensure_dir_exists;
use anyhow::Result;
use std::path::Path;

/// Per-app SELinux rules are gone from the fork: nothing reads the per-app policy files any
/// more. The subcommand keeps its shape, and its `Result`, so a caller that still invokes it
/// exits 0 instead of failing. Module `sepolicy.rule` files are a different path and still load.
#[allow(clippy::unnecessary_wraps)]
pub fn set_sepolicy(_pkg: String, _policy: String) -> Result<()> {
    Ok(())
}

/// The read side of the same no-op. It prints nothing rather than failing, so
/// `profile get-sepolicy <pkg>` yields an empty string.
#[allow(clippy::unnecessary_wraps)]
pub fn get_sepolicy(_pkg: String) -> Result<()> {
    Ok(())
}

// ksud doesn't guarteen the correctness of template, it just save
pub fn set_template(id: String, template: String) -> Result<()> {
    ensure_dir_exists(defs::PROFILE_TEMPLATE_DIR)?;
    let template_file = Path::new(defs::PROFILE_TEMPLATE_DIR).join(id);
    std::fs::write(template_file, template)?;
    Ok(())
}

pub fn get_template(id: String) -> Result<()> {
    let template_file = Path::new(defs::PROFILE_TEMPLATE_DIR).join(id);
    let template = std::fs::read_to_string(template_file)?;
    println!("{template}");
    Ok(())
}

pub fn delete_template(id: String) -> Result<()> {
    let template_file = Path::new(defs::PROFILE_TEMPLATE_DIR).join(id);
    std::fs::remove_file(template_file)?;
    Ok(())
}

pub fn list_templates() -> Result<()> {
    let templates = std::fs::read_dir(defs::PROFILE_TEMPLATE_DIR);
    let Ok(templates) = templates else {
        return Ok(());
    };
    for template in templates {
        let template = template?;
        let template = template.file_name();
        if let Some(template) = template.to_str() {
            println!("{template}");
        }
    }
    Ok(())
}

/// A no-op under its original name, so the two boot-stage call sites next to
/// `module::load_sepolicy_rule()` do not move. That different path still has to run.
#[allow(clippy::unnecessary_wraps)]
pub const fn apply_sepolies() -> Result<()> {
    Ok(())
}
