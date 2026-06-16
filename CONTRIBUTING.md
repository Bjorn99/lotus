# Contributing to Lotus

Thanks for thinking about contributing. This is a community fork maintained by one person, so a few things work differently from larger projects.

## What kind of contributions are welcome

**Leaf-level changes** — bug fixes, small features, UI polish, import/export improvements. Things at the edges of the codebase that don't change core data structures or build tooling.

**Before opening a PR** for anything larger than a bug fix, open an issue and let's talk first. That avoids you spending time on something that may not fit.

## What needs discussion first

The core of the app — the domain model, database schema, build configuration, dependency versions — is intentionally kept stable. If your change touches these, or if you're not sure, open an issue before writing code.

The project follows a spec-first workflow for features: brainstorm → spec → plan → implement. If you're proposing a new feature, expect some back-and-forth before code is written.

## House style

- Keep changes focused. A PR that fixes a bug and also reformats three files won't be merged — the reformatting makes it hard to see what actually changed.
- Leave comments out unless the _why_ would surprise someone reading the code cold.
- Match the existing code's style, even if you'd write it differently.
- Write tight — if it can be 50 lines instead of 200, it should be.

## PRs may be re-implemented

This project has a high bar for code quality and consistency. It's normal for a PR to be closed with thanks and re-implemented by the maintainer. When that happens, you'll be credited in the commit message and the changelog. Your contribution isn't wasted — you surfaced a real problem and often the fix is exactly what you wrote, just restructured to fit the codebase.

## Tests

If you're fixing a bug, include a test that reproduces it. If you're adding a feature, include a test that covers the happy path. The test suite runs with `./gradlew testDebugUnitTest`.

## Attribution

If the maintainer re-implements your PR, credit goes in the commit message itself (e.g., "Based on a patch by @username").

## Legal

By opening a PR, you agree to license your contribution under the same license as the project (GPL-3.0).
