# Releasing

How to build and publish a Lotus release. The CI pipeline handles the
heavy lifting — this doc covers what you do locally and what the
automation does with it.

## Overview

A release has three steps:

1. Bump the version, update the changelog, commit, and tag
2. Push the tag — CI builds, signs, and publishes to GitHub Releases
3. F-Droid picks up the new tag from the [fdroiddata repo](https://gitlab.com/BranaX/fdroiddata)

F-Droid is the only distribution channel. No manual APK uploads to other
stores.

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
    versionCode = 1_007_000   // increment by 1_000 per release
    versionName = "1.7.0-community"
    ...
}
```

The `-community` suffix is mandatory — it distinguishes this fork's
builds from upstream.

**versionCode formula:** `major * 1_000_000 + minor * 1_000 + patch`.
So v1.7.0 is `1_007_000`, v1.7.1 is `1_007_001`, etc. CI appends the
ABI code at build time, so the final versionCode on the APK is higher
than this.

## Step 2: update the changelog

Add a new section to `CHANGELOG.md` using the version **without** the
`-community` suffix:

```markdown
## 1.7.0 — Short summary of this release

[changes written for end users, one sentence per line]
```

The CI workflow extracts this section verbatim as the GitHub Release
body. If the section heading doesn't match the tag (e.g. tag is
`v1.7.0-community` but the heading is `## 1.7.0`), the release job
fails before building.

## Step 3: commit and tag

```bash
git add app/build.gradle.kts CHANGELOG.md
git commit -m "bump version to v1.7.0-community (1007000)"
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

1. **Preflight** — verifies the tag matches `versionName` and that a
   matching `## X.Y.Z` section exists in `CHANGELOG.md`
2. **Unit tests** — runs `./gradlew :app:testDebugUnitTest`
3. **Assemble** — builds per-ABI APKs (`armeabi-v7a`, `arm64-v8a`,
   `x86_64`) plus a universal APK, all signed with the release keystore
4. **Verify** — runs `apksigner verify` on every APK to confirm the
   signature is intact
5. **Checksum** — generates `SHA256SUMS.txt` for all APKs
6. **Publish** — creates a GitHub Release with the changelog section as
   the body, attaches all APKs and the checksum file

You can watch the progress at:
`https://github.com/Bjorn99/lotus/actions`

## Step 5: F-Droid picks it up

After the GitHub Release is published, update the
[fdroiddata](https://gitlab.com/BranaX/fdroiddata) repo. The F-Droid
build server monitors that repo and will build the new version from
source.

The update typically takes a few days from MR merge to appearing in the
F-Droid client.

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
