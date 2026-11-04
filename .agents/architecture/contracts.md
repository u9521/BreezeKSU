# Cross-layer contracts

Covers: uapi/, kernel/include/*, .gitignore

The contract between kernel, daemon and app. These are the invariants that make a change in one layer a change in three.

## UAPI version is a handshake, not a hint

Three places must agree on `KERNEL_SU_UAPI_VERSION`:

- `uapi/supercall.h` — the kernel's declaration.
- The bindings in `userspace/ksud/src/ksucalls.rs` — `ksud` refuses to run when they differ from the kernel's answer.
- `MINIMAL_SUPPORTED_KERNEL` / `managerUAPIVersion` in the app's `Natives.kt` — the app degrades to a reduced UI when they do not match.

A bump therefore ships kernel, daemon and app together. Any partial release leaves a device where one layer refuses to talk to another, and the visible symptom (a missing tab, a command that errors) does not point at the version skew.

The anchor for the whole fork is recorded in `doc/PRUNING.md`; the sync gate reads it from there.

## Shared headers are symlinked

`kernel/include/uapi` and `manager/app/src/main/cpp/uapi` are symlinks to the repository's `uapi/`. Some checkouts and copy tools replace a symlink with a plain directory copy, which turns every shared header into a duplicated file and produces a `120000 => 100644` mode change in the diff.

If a diff shows that mode change, restore the symlink rather than committing the copy.

## Legacy command numbers stay reserved

`GET_INFO_LEGACY` and the older allowlist getters remain in the ABI even when the fork stops using them: an old app on a new kernel must still get a well-defined answer. Removing a command means leaving its number unused, never reusing it — see [kernel](kernel.md).

## Git must work from both operating systems

`.git/objects/info/alternates` must hold a relative path, not an absolute one. The app build runs Gradle on Windows while the repository lives on a WSL filesystem; a Windows git that cannot read the alternates returns nothing for `git rev-list --count HEAD`, and the build fails with an unrelated-looking parse error from the Gradle script.

`.gitignore` controls `CLAUDE.md` and `AGENTS.md` at the repository root. Both are force-added when they exist, so ignore rules never hide a live file from `git status`.

## Boundaries

- New shared structures are declared in `uapi/` only; a private header used by two layers is a contract that has not been named yet.
- Anything versioned in `uapi/` gets a decision entry in `doc/decisions/` before the bump lands.
- Layer-local helpers stay layer-local; sharing one is a contract change and needs the same three-layer review.
