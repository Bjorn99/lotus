# Distribution channels

This fork is published as signed APKs through GitHub Releases. This
document covers the two additional channels we want to support:

- [F-Droid](#f-droid) — community-curated FOSS app store. Auto-builds
  from this repo on every signed tag.
- [itch.io](#itchio) — indie hosting. Manual upload of the same APKs
  we already publish on GitHub Releases.

The GitHub Releases pipeline is already documented in the README's
**"Cutting a public release"** section. Don't duplicate that here.

---

## F-Droid

F-Droid runs its own build farm and rebuilds every release from
source. We don't push APKs to F-Droid — we hand over a metadata recipe
that tells the F-Droid builder how to build us, and they take it from
there.

### One-time setup

The fork's `applicationId` is `com.dn0ne.lotus.community`. Upstream
already has `com.dn0ne.lotus` on F-Droid, so we have to submit a
**new** F-Droid app entry rather than co-opting the existing one.

1. **Read the F-Droid Inclusion Policy.**
   <https://f-droid.org/docs/Inclusion_Policy/>. Lotus passes —
   GPLv3, no proprietary deps, no telemetry, no analytics, no ad SDK,
   no anti-features that need disclosing other than `NonFreeNet` for
   the LRCLIB and MusicBrainz lookups (both optional, both clearly
   labelled in-app).

2. **Fork `fdroiddata`** on GitLab.
   <https://gitlab.com/fdroid/fdroiddata>. This is the canonical
   recipe repo.

3. **Add a metadata file** at
   `metadata/com.dn0ne.lotus.community.yml` in your fork. Skeleton:

   ```yaml
   Categories:
     - Multimedia
   License: GPL-3.0-only
   AuthorName: Bjorn99 (community fork)
   AuthorWebSite: https://github.com/Bjorn99
   SourceCode: https://github.com/Bjorn99/lotus
   IssueTracker: https://github.com/Bjorn99/lotus/issues
   Changelog: https://github.com/Bjorn99/lotus/blob/master/CHANGELOG.md

   AutoName: Lotus
   Description: |-
       Community continuation of Lotus by dn0ne. Local music player
       with Material You theming, LRCLIB synced lyrics, MusicBrainz
       metadata search, M3U export, smart playlists, global library
       search. No telemetry, no analytics, no network calls except
       LRCLIB / MusicBrainz lookups (both optional).

   AntiFeatures:
     - NonFreeNet  # LRCLIB / MusicBrainz queries

   RepoType: git
   Repo: https://github.com/Bjorn99/lotus.git

   Builds:
     - versionName: 1.3.6-community
       versionCode: 1003006
       commit: v1.3.6
       subdir: app
       gradle:
         - yes
       prebuild:
         - chmod +x gradlew
       output: build/outputs/apk/release/lotus-${{ versionName }}-universal-release.apk

   AutoUpdateMode: Version v%v
   UpdateCheckMode: Tags ^v[0-9.]+$
   CurrentVersion: 1.3.6-community
   CurrentVersionCode: 1003006
   ```

   Field guide:
   - `Categories` — must be one of the F-Droid categories. `Multimedia`
     is right.
   - `AntiFeatures: NonFreeNet` — required because LRCLIB and
     MusicBrainz are external services. If a future feature adds more
     non-free network use, declare it.
   - `Builds` — the recipe is appended to per release; F-Droid only
     builds versions listed here.
   - `AutoUpdateMode: Version v%v` — F-Droid's `fdroidserver` tooling
     will auto-append a new `Builds:` entry for each new tag matching
     the `UpdateCheckMode` regex, so we don't manually edit this file
     for every release after the initial submission.

4. **Open a merge request** against `fdroiddata`. The F-Droid
   maintainers usually respond within a week. They may ask for
   tweaks (categorisation, anti-feature list, build issues).

5. **Once merged**, F-Droid's build farm picks up the recipe on the
   next nightly run. The app appears at
   `https://f-droid.org/packages/com.dn0ne.lotus.community/` after a
   build succeeds.

### Each release — what F-Droid does automatically

After the initial recipe is in `fdroiddata`:

1. We push a `v*` tag to this repo. Our own
   [`release.yml`](../.github/workflows/release.yml) signs and
   publishes APKs to GitHub Releases (unchanged).
2. F-Droid's `fdroidserver` autoupdate scans this repo's tags every
   24h. New tag matching `UpdateCheckMode: Tags ^v[0-9.]+$` triggers
   a new `Builds:` entry being appended to the recipe.
3. F-Droid's build farm runs `./gradlew assembleRelease`,
   strips/inspects the APK, signs it with **F-Droid's signing key**
   (not ours), and publishes to f-droid.org.

### F-Droid signs with their own key — implications

- The APK on F-Droid has a **different signature** from the one on
  our GitHub Releases page. Android treats them as different apps;
  users can't migrate between channels without uninstalling.
- This is normal for F-Droid; it's a security model, not a bug.
- Document this on the Releases page so users pick a channel and
  stick with it.

### Reproducible builds (optional but encouraged)

F-Droid will accept us either way, but a reproducible build means
they can compare their build's APK to ours byte-for-byte and avoid
the dual-signing situation. For Lotus to be reproducible:

- No timestamps in the APK (`includeBuildTypes` already strips
  some — Gradle's archive task respects `SOURCE_DATE_EPOCH`).
- `dependenciesInfo { includeInApk = false; includeInBundle = false }`
  is already set in our `app/build.gradle.kts`. Good.
- Pin AGP, Kotlin, all dependency versions in `libs.versions.toml`
  exactly. Already done.

When ready for reproducible mode, add `Binaries:` to the recipe:

```yaml
Binaries: https://github.com/Bjorn99/lotus/releases/download/v%v/lotus-%v-universal-release.apk
AllowedAPKSigningKeys: <sha256 of our release keystore>
```

Find the SHA-256 of our keystore certificate via:

```fish
keytool -list -v -keystore ~/keystores/lotus-release.jks -alias lotus | grep "SHA256:"
```

### Maintaining the F-Droid recipe

- Don't manually edit the recipe file in `fdroiddata` for routine
  releases — `AutoUpdateMode` does it for you.
- Edit only when you need to: (a) declare a new anti-feature, (b)
  bump `targetSdk` requirements, (c) add a new locale to
  `Localized:`, (d) change the description.
- Open one MR per change.

---

## itch.io

itch.io is informal compared to F-Droid. There's no review, no
re-build, no signing intermediary. We upload our existing GitHub
Release APKs and itch hosts them.

### One-time setup

1. Create an itch.io account (or use an existing one).
2. Click **Upload new project** at
   <https://itch.io/game/new>. Despite the URL, "game" supports
   apps — pick `Tools` for `Kind of project`.
3. Fill in:
   - **Title**: `Lotus (community fork)`
   - **Project URL**: `https://bjorn99.itch.io/lotus`
   - **Short description**: `Local music player for Android. Material
     You, LRCLIB synced lyrics, smart playlists, M3U export. No
     telemetry. Community continuation of dn0ne/lotus.`
   - **Classification**: `Tools`
   - **Kind of project**: `Downloadable` → tick `This project is
     primarily an Android app`.
   - **Pricing**: `No payments`. (We're GPL — no obligation but no
     point monetising upstream's work either.)
   - **Uploads**: drag in the universal APK from the latest GitHub
     Release. Mark it **Android** in the platform checkbox.
   - **Cover image**: `fastlane/metadata/android/en-US/images/icon.png`
   - **Screenshots**: any 4-5 from
     `fastlane/metadata/android/en-US/images/phoneScreenshots/`
4. **Visibility**: leave on **Draft** until the page looks right,
   then flip to **Public**.

### Each release — manual upload

1. After tagging `v1.3.7`, download the universal APK from
   `https://github.com/Bjorn99/lotus/releases/download/v1.3.7/lotus-1.3.7-community-universal-release.apk`.
2. itch.io project page → **Edit game** → **Uploads** → **Upload
   files** → drag the APK in.
3. Tick **This file will be played in the browser** = **No**.
4. Tick the **Android** platform checkbox.
5. Click **Save**.
6. (Optional) Toggle the previous version's **Display this download**
   off if you want only the latest visible.

### Each release — automated upload via butler

itch.io's CLI is called `butler`. It's optional; saves clicks if you
ship often.

```fish
# one-time install (Arch / Garuda):
yay -S itch-butler   # or download from https://itch.io/docs/butler/installing.html

# one-time login (browser flow):
butler login

# upload after each tag:
gh release download v1.3.7 \
    --repo Bjorn99/lotus \
    --pattern 'lotus-*-universal-release.apk' \
    --dir /tmp
butler push /tmp/lotus-1.3.7-community-universal-release.apk \
    bjorn99/lotus:android \
    --userversion 1.3.7
```

The `bjorn99/lotus:android` argument is `<account>/<project>:<channel>`.
Channels are itch.io-specific labels — `:android` is convention for
Android APKs. Multiple channels are fine if you ever ship per-ABI
APKs separately.

### CI integration (later)

If we want fully-automated itch.io publishing on every tag, add a
`butler push` step to `.github/workflows/release.yml` after the
GitHub Release is created. This needs:

- A `BUTLER_API_KEY` repo secret (generate at
  <https://itch.io/user/settings/api-keys>).
- The `butler` binary on the runner (cached or downloaded each run).

Skipped for now because itch.io is a "nice to have" channel; the
manual flow above is fine for releases that already get GitHub +
F-Droid.

---

## Channel comparison

| | GitHub Releases | F-Droid | itch.io |
|---|---|---|---|
| Signed by | us | F-Droid | us (we upload our signed APK) |
| Build run by | our CI | F-Droid build farm | n/a (we upload pre-built) |
| Publishes new tag in | minutes (CI run time) | up to 24h after autoupdate scan + their nightly build | minutes (manual or butler) |
| Updates auto-detected on phone | only if user has [Obtainium](https://github.com/ImranR98/Obtainium) | yes (F-Droid client) | no — user has to revisit page |
| Per-ABI APKs | yes (5 + universal) | no — F-Droid serves universal only by default | typically just universal |
| Verifiable against source | manually | automatically | manually |

Recommendation: GitHub Releases is the canonical channel. F-Droid is
the recommended one to point new users at because of the auto-update
support. itch.io is optional, low-effort discoverability for users
who already have an itch account.
