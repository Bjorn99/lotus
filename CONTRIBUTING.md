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

## Translations

Translations are always welcome and are one of the easiest ways to help.

Lotus is translated through Android string resources. All translatable text lives in `app/src/main/res/values/strings.xml` (English — the source of truth). A translation is a parallel file at `app/src/main/res/values-<lang>/strings.xml`, for example `values-es` for Spanish or `values-ru` for Russian.

To add or update a translation:

- Copy `values/strings.xml` to `values-<lang>/strings.xml` and translate the text **between** the tags. Leave every `name="..."` key exactly as it is — the app looks strings up by key, so a changed key breaks that string.
- Keep placeholders (`%1$s`, `%1$d`, `%%`), line breaks (`\n`), and any `<b>`/`<i>` tags unchanged. Escape a literal apostrophe as `\'` and a literal quote as `\"`.
- The `<plurals>` block needs the quantity forms your language uses (English and Spanish use `one` and `other`; some languages need more).
- Don't translate the app name or the URL strings — they're marked `translatable="false"` and stay as-is.
- **Partial is fine.** Anything you don't translate falls back to English automatically, so you don't have to finish every string to open a useful PR.

Send it as a pull request (preferred). If you'd rather not use git, attach the file to an issue and it'll be committed with credit to you. If copying the file yourself is a hassle, ask the maintainer for a ready-to-fill scaffold of the current strings.

## Attribution

If the maintainer re-implements your PR, credit goes in the commit message itself (e.g., "Based on a patch by @username"), in the release changelog, and in the Contributors list below.

## Contributors

Thanks to everyone who has helped improve this fork:

- **[@uhrfra](https://github.com/uhrfra)** — relative-path support in M3U playlist import (based on [#73](https://github.com/Bjorn99/lotus/pull/73), shipped in v1.8.0); on-device testing of the v1.8.2 fixes.

## Legal

By opening a PR, you agree to license your contribution under the same license as the project (GPL-3.0).
