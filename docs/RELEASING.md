# Releasing

How to build and publish a Lotus release. The CI pipeline handles the
heavy lifting — this doc covers what you do locally and what the
automation does with it.

## Overview

A release has three steps:

1. Bump the version, update the changelog, commit, and tag
2. Push the tag — CI builds, signs, and publishes to GitHub Releases
3. F-Droid's `checkupdates bot` notices the tag and updates its own metadata — you do nothing

APKs reach users two ways: the GitHub Release the workflow publishes, and
F-Droid. There are no manual uploads to any app store.

## Before you start

Make sure you're on `master` and the working tree is clean:

```bash
git checkout master
git pull origin master
git status          # should show "nothing to commit, working tree clean"
```

## Step 1: bump the version

Edit `app/build.gradle.kts`. Two numbers need to change:

```kotlin
defaultConfig {
    versionCode = 1_007_000   // see the formula below
    versionName = "1.7.0-community"
    ...
}
```

The `-community` suffix is mandatory — it distinguishes this fork's
builds from upstream.

**versionCode formula:** `major * 1_000_000 + minor * 1_000 + patch`.
So v1.7.0 is `1_007_000`, v1.7.1 is `1_007_001`, v1.9.1 is `1_009_001`.

The **universal** APK ships this number unchanged. Only the per-ABI APKs get
an offset added at build time, from `abiCodes` in `app/build.gradle.kts`:
armeabi-v7a `+1`, arm64-v8a `+2`, x86 `+3`, x86_64 `+4`. Measured on the
published 1.9.1 APKs: universal `1009001`, arm64-v8a `1009003`, x86_64
`1009005`.

Because the offsets span 4 and consecutive releases differ by 1, a *lower*
ABI's APK in a new release can carry a versionCode below a *higher* ABI's from
the previous one — 1.9.1 universal is `1009001`, while 1.9.0 arm64-v8a was
`1009002`. Upgrades within one ABI always increase, so this only matters to
someone switching APK flavour between releases.

## Step 2: update the changelog

Add a new section to `CHANGELOG.md` using the version **without** the
`-community` suffix:

```markdown
## 1.7.0

[changes written for end users, one long line per bullet]
```

Never hard-wrap a bullet across lines — one long line per bullet, however
long, and no indented continuation lines. The device wraps it, you don't.
Hard-wrapped bullets render as visible mid-sentence breaks on F-Droid and
GitHub mobile.

**The heading drops the `-community` suffix, and that is correct.** The
workflow computes `CHANGELOG_VERSION="${VERSION%-community}"`
([release.yml:112](../.github/workflows/release.yml#L112)), so the tag
`v1.7.0-community` looks for the heading `## 1.7.0`. The job fails before
building only if no such heading exists.

### The F-Droid changelog — easy to forget, and separate

F-Droid shows its own release notes, from a file named for the **versionCode**:

```
fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
```

1.9.1 (`versionCode = 1_009_001`) needed
`fastlane/metadata/android/en-US/changelogs/1009001.txt`.

F-Droid caps this at **500 characters**
([F-Droid docs](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/)).
Count characters, not bytes — the two diverge as soon as the text is
non-ASCII (`1008002.txt` is 383 bytes but 369 characters):

```bash
wc -m fastlane/metadata/android/en-US/changelogs/1009001.txt
```

Commit this file **before** tagging, so it is present in the tagged tree.

## Step 3: commit and tag

```bash
git add app/build.gradle.kts CHANGELOG.md \
    fastlane/metadata/android/en-US/changelogs/1007000.txt
git commit -m "chore: bump to 1.7.0-community and add the F-Droid changelog"
git tag -a v1.7.0-community -m "Lotus 1.7.0"
```

**The tag name must match the `versionName` exactly**, including the
`-community` suffix. `v1.7.0-community` in the tag ↔
`versionName = "1.7.0-community"` in build.gradle.kts. The CI preflight
step checks this and aborts if they don't match.

Push:

```bash
git push origin master
git push origin v1.7.0-community
```

## Step 4: CI builds the release

Pushing the tag triggers [`.github/workflows/release.yml`](../.github/workflows/release.yml).
The workflow:

1. **Preflight, three checks** — all four signing secrets are present; the
   tag matches `versionName`; and `CHANGELOG.md` has a matching `## X.Y.Z`
   section. Each fails the job before anything is built
2. **Unit tests** — `./gradlew testDebugUnitTest --stacktrace`
3. **Assemble** — `./gradlew assembleRelease --stacktrace`, producing four
   per-ABI APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) plus a
   universal APK, all signed with the release keystore
4. **Verify** — `apksigner verify --verbose --print-certs` on every APK.
   Read the certificate DN in the log: it must be your release key, never
   `CN=Android Debug`
5. **Checksum** — generates `SHA256SUMS.txt` for all APKs
6. **Publish** — creates a GitHub Release whose body is the fork preamble
   followed by the changelog section, attaching all five APKs and the
   checksum file

You can watch the progress at:
`https://github.com/Bjorn99/lotus/actions`

## Step 5: F-Droid picks it up — nothing to do

**Do not hand-edit fdroiddata, and do not push to a personal fork of it.**

Lotus's entry upstream at
[fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.dn0ne.lotus.community.yml)
is configured for automatic updates:

```yaml
AutoUpdateMode:  "Version"
UpdateCheckMode: "Tags"
```

`UpdateCheckMode: Tags` makes F-Droid watch this repo's tags.
`AutoUpdateMode: Version` makes it add the new `Builds:` entry and bump
`CurrentVersion` on its own.

That file has **12 commits in total: 10 by `checkupdates bot`** (every version
from 1.5.9 through 1.9.0) **and 2 by humans** — the original submission
(Bjorn, 2026-05-19) and a maintainer follow-up the next day
(Licaon_Kter, "lotus - keep latest", 2026-05-20). No release has needed a
human touch there since.

Measured lag from tag to bot commit:

| Tag | Tagged | Bot commit | Lag |
|---|---|---|---|
| v1.7.0 | 2026-06-08 | 2026-06-09 | 1 day |
| v1.8.0 | 2026-06-22 | 2026-06-23 | 1 day |
| v1.8.3 | 2026-07-24 | 2026-07-25 | 1 day |
| v1.9.0 | 2026-08-15 | 2026-08-15 | same day |

That lag is metadata only. How long the build and publish cycle then takes to
surface the update in the F-Droid client has not been measured here.

An earlier revision of this doc said to update fdroiddata manually, via a
personal fork. That was wrong: those pushes were never merged upstream, and
the bot did the work from the tag regardless. Following it cost a detour
during the 1.9.0 release.

## Local signed builds (optional)

If you want to build a signed APK locally instead of through CI, create
`keystore.properties` at the repo root (gitignored):

```properties
storeFile=/absolute/path/to/lotus-release.jks
storePassword=your-store-password
keyAlias=lotus
keyPassword=your-key-password
```

Or set the same values as environment variables:
`LOTUS_KEYSTORE_FILE`, `LOTUS_KEYSTORE_PASSWORD`, `LOTUS_KEY_ALIAS`,
`LOTUS_KEY_PASSWORD`.

If neither is configured, `assembleRelease` falls back to the debug
keystore with a visible warning — **do not distribute those APKs.**

Then:

```bash
./gradlew :app:assembleRelease
```

APKs land in `app/build/outputs/apk/release/`.

## CI secrets

The release workflow needs four secrets in the GitHub repo under
**Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `LOTUS_KEYSTORE_BASE64` | `base64 -w0 lotus-release.jks` |
| `LOTUS_KEYSTORE_PASSWORD` | Keystore password |
| `LOTUS_KEY_ALIAS` | Key alias (`lotus`) |
| `LOTUS_KEY_PASSWORD` | Key password |

The decoded keystore file is scrubbed from the runner immediately after
signing.

## Generating a release keystore

One-time setup. Keep the generated file off GitHub and back it up
securely — losing it means you can never publish an update under the
same signature.

```bash
keytool -genkeypair -v \
  -keystore lotus-release.jks \
  -alias lotus \
  -keyalg RSA -keysize 2048 -validity 10000
```

Save `lotus-release.jks` somewhere outside the repo. Encode it for the
CI secret:

```bash
base64 -w0 lotus-release.jks
```

Copy the output into the `LOTUS_KEYSTORE_BASE64` secret.
