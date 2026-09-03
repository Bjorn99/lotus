# Contributing to Lotus

Thanks for thinking about contributing. This is a community fork, so a few things work differently from larger projects.

## What kind of contributions are welcome

**Leaf-level changes** — bug fixes, small features, UI polish, import/export improvements. Things at the edges of the codebase that don't change core data structures or build tooling.

## A few practical things

This is a small project — a little rhythm goes a long way:

- **One focused PR at a time.** Please don't open a stack of PRs across different parts of the app at once — a large pile is hard to review well, and the ones that need discussion end up blocking the ones that don't.
- **An issue before code** for anything beyond a small bug fix or a translation. It's the cheapest way to find out whether something fits before you spend time on it.
- **Reviews take time.** A PR may sit for a while, come back with questions, or be re-implemented and merged under your name. None of that means it wasn't worth opening.

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
- **One language per pull request**, and only languages you speak fluently. Every release adds new English strings, so a translation needs someone who can keep it current, not just a first pass.
- **Don't add keys that aren't in `values/strings.xml`.** Anything extra either does nothing, or silently overrides a string belonging to one of the app's libraries.

Send it as a pull request (preferred). If you'd rather not use git, attach the file to an issue and it'll be committed with credit to you. If copying the file yourself is a hassle, ask the maintainer for a ready-to-fill scaffold of the current strings.

## Attribution

If the maintainer re-implements your PR, credit goes in the commit message itself (e.g., "Based on a patch by @username"), in the release changelog, and in the contributors list.

## Contributors

The contributors list lives on the [README](README.md#contributors), where people actually see it.

## Legal

By opening a PR, you agree to license your contribution under the same license as the project (GPL-3.0).
