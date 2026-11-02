---
name: pre-push-checks
description: Use before committing or pushing changes in this repository — picks the smallest check set that covers the actual diff (Rust Android compile, Kotlin app compile, spec gates, on-device smoke) and runs it in order. Triggered by "commit", "push", "pre-push", "is this safe to land", or "what else do I need to run".
---

# Pre-push checks

The goal: cover the real risk surface with the **smallest** command set, and report results honestly. Do not re-run everything to look thorough, and never claim a check passed without running it.

## Steps

1. Establish the diff:

   ```sh
   git status --short && git diff --stat && git diff --cached --stat
   ```

2. Take the union of the commands below that match the diff, and run them left to right.
3. Stop on the first failure, locate the file and line, fix, then re-run that group from the start. Do not skip ahead.
4. Report each command with its result; state the reason for anything skipped.

## Matrix

| What changed | Must run |
| :--- | :--- |
<!-- repo:begin -->
| `AGENTS.md`, `.agents/**`, `doc/**` | `node .agents/gates/verify-all.mjs` |
| `userspace/**` | `cd userspace/ksud && cargo ndk -t arm64-v8a check` (or `cross build --target aarch64-linux-android`), then `node .agents/gates/verify-all.mjs` |
| `manager/**` | stage `jniLibs/libksud.so` for each ABI in `abiFilters`, then `cd manager && ./gradlew :app:assembleDebug` |
| `kernel/**`, `uapi/**` | no local compile is possible; run `breeze-sync-gates` G1 and G2 plus one on-device smoke run |
| `website/**` | `cd website && bun run docs:build` |
| `js/**` | the package's own lint/test scripts |
<!-- repo:end -->

The framework is verified by `node .agents/gates/verify-all.mjs`; the project is verified by its own builds. Neither substitutes for the other — the gates only cover the framework's documentation and scaffold.

## Hard prohibitions

- Do not relax a budget, delete a required section or skip a script to make a gate green; a red gate means the content is wrong.
- Do not commit uncontrolled build artifacts or generated files. Note that `jniLibs/` is git-ignored by design.
- Do not mix formatting and logic in one commit; split or revert the incidental diff.

## Reporting

- All green: list the commands run and their key output (for example the gate's `N 个文件在预算内`).
- Failure: quote the failing line, explain the root cause and the fix. Do not paste whole logs.
- Checks that cannot run here (no target kernel tree, no Android device): say **"not verified"** explicitly and state what that leaves exposed.
