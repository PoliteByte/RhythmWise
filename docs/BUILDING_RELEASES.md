# Building Releases

This guide describes the end-to-end process for shipping a RhythmWise release. Every release is published to **two channels in parallel** using the same version and tag:

- **GitHub Releases** — a signed APK attached to a tagged release, for users who sideload (privacy-focused users, GrapheneOS, regions without Play Store access).
- **Google Play Store** — a signed AAB uploaded to Play Console, for the typical Android user who installs and auto-updates via the Play Store.

This is the per-release workflow. For one-time setup (LLC, Play Console account, store listing, keystore generation) see `docs/PLAY_STORE_PUBLISHING_GUIDE_ORG.md`. For the commit-message format used in the release commit, see `docs/GIT_COMMIT_GUIDELINES.md`.

---

## Prerequisites

These are one-time setup items. Once done, skip ahead to [Per-release workflow](#per-release-workflow).

| Item | Where | Notes |
|------|-------|-------|
| Release keystore (`.jks`) | Local disk, **not** in the repo | Generate per `docs/PLAY_STORE_PUBLISHING_GUIDE_ORG.md` §3. Back up to encrypted offline storage. |
| Signing credentials | `local.properties` (gitignored) | `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. |
| `signingConfigs.release` block | `composeApp/build.gradle.kts` | Reads from `local.properties`. See §3 of the Play Store guide for the template. |
| Git authentication | SSH key on GitHub | Use SSH rather than HTTPS — Windows Credential Manager can cache stale creds that survive repo transfers and silently 403 on push. See [Git auth — SSH setup](#git-auth--ssh-setup) below. |
| GitHub CLI | Local install | Used for creating releases. Install: `winget install GitHub.cli`, then `gh auth login`. |
| Play Console access | Browser | Required for the AAB upload. |
| Android SDK Build Tools | Local install | Provides `apksigner` for signature verification. |

### Git auth — SSH setup

One-time setup. SSH keys do not expire and bypass the Windows credential helper entirely.

```powershell
ssh-keygen -t ed25519 -C "<your-github-email>"
Get-Content ~/.ssh/id_ed25519.pub | Set-Clipboard
```

Add the copied public key at https://github.com/settings/keys → **New SSH key**.

Switch this clone's remote from HTTPS to SSH:

```powershell
git remote set-url origin git@github.com:PoliteByte/RhythmWise.git
ssh -T git@github.com
```

The SSH test should reply `Hi <your-github-username>! You've successfully authenticated...`. On first connection, accept GitHub's host key with `yes`.

---

## Per-release workflow

The steps below assume all PRs for this release have already merged into `dev` and `dev` is the state you want to ship. The flow is:

1. Bump the version on a branch off `dev`, PR into `dev`.
2. Open a `dev → main` PR to promote the release-ready state.
3. Tag the merge commit on `main`.

Both `dev` and `main` are protected — every step happens through PRs. The bump is its own isolated PR (not bundled into a feature PR) so that the release commit is clean and easy to revert if needed.

### Step 1: Decide the version bump

Determine the new version based on what shipped since the last release:

- **Beta increment** — `1.0.0-beta.1` → `1.0.0-beta.2`
- **Beta to stable** — `1.0.0-beta.3` → `1.0.0`
- **Patch** — `1.0.0` → `1.0.1` (bug fixes only)
- **Minor** — `1.0.0` → `1.1.0` (backward-compatible features)
- **Major** — `1.0.0` → `2.0.0` (breaking changes)

`versionCode` is independent of the SemVer string — it is a monotonically increasing integer required by Google Play. Increment it by 1 every release regardless of bump type.

### Step 2: Create a release branch off `dev`

```powershell
git checkout dev
git pull
git checkout -b <issue#>-chore-release-vX.Y.Z
```

If no issue number applies, use `chore-release-vX.Y.Z`.

### Step 3: Bump the version in `composeApp/build.gradle.kts`

Edit lines 100-101:

```kotlin
versionCode = 3            // was 2 — increment by 1
versionName = "1.0.0-beta.3"   // new SemVer string
```

### Step 4: Generate the commit summary since the last tag

Run the snippet below to dump commits since the previous tag, grouped by Conventional-Commit type. Use the output as raw material for `docs/RELEASE_NOTES.md` — do **not** paste it in verbatim; rewrite it into user-facing language.

```powershell
$prev = git describe --tags --abbrev=0
$commits = git log "$prev..HEAD" --no-merges --pretty=format:'%s' | ForEach-Object { $_.Trim() }
$groups = @(
    @{ Type = 'feat';     Header = 'Features' },
    @{ Type = 'fix';      Header = 'Bug fixes' },
    @{ Type = 'perf';     Header = 'Performance' },
    @{ Type = 'refactor'; Header = 'Refactors' },
    @{ Type = 'test';     Header = 'Tests' },
    @{ Type = 'chore';    Header = 'Chores' }
)
$consumed = @{}
foreach ($g in $groups) {
    $pattern = "^$($g.Type)(\(|:|!)"
    $matched = $commits | Where-Object { $_ -match $pattern }
    if ($matched) {
        Write-Output ""
        Write-Output "### $($g.Header)"
        $matched | ForEach-Object { Write-Output "- $_"; $consumed[$_] = $true }
    }
}
$other = $commits | Where-Object { -not $consumed.ContainsKey($_) }
if ($other) {
    Write-Output ""
    Write-Output "### Other"
    $other | ForEach-Object { Write-Output "- $_" }
}
```

The snippet trims leading whitespace from subject lines (some older commits have it) and puts anything that doesn't match a known type into an **Other** bucket so non-conventional commits aren't silently dropped. Save the output somewhere temporary — you will reference it in the next step.

### Step 5: Update `docs/RELEASE_NOTES.md`

The release notes serve **two audiences**: users browsing the GitHub Release page, and users reading the Play Store "What's new" section. Keep them user-facing and narrative — not a raw commit dump.

1. Replace the version heading on line 1 (e.g., `## v1.0.0-beta.3`).
2. Rewrite the **What's New** section using the commit summary from Step 4. Focus on what the user will notice. Drop refactors, chores, and internal-only fixes unless they have user-visible impact (e.g., a perf improvement).
3. Review the **Features** list — add new features, remove anything removed, fix any inaccuracies.
4. If moving from beta to stable, remove the beta notice at the bottom of the file.

A short, well-curated entry is better than a long unedited list. The Play Store "What's new" field is capped at 500 characters; aim to keep the headline section under that.

### Step 6: Run tests and static analysis

```powershell
./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
./gradlew :composeApp:lintDebug
./gradlew detekt
```

All checks must pass before proceeding. Fix any failures — never skip them.

### Step 7: Commit, push, and open a release PR

Stage only the two files you changed:

```powershell
git add composeApp/build.gradle.kts docs/RELEASE_NOTES.md
git commit -s -m "chore(release): bump version to 1.0.0-beta.3"
```

The `-s` flag adds the required `Signed-off-by:` line. Push and open a PR targeting `dev`:

```powershell
git push -u origin <branch-name>
gh pr create --base dev --title "chore(release): bump version to 1.0.0-beta.3" --fill
```

Wait for CI (`test.yml`) to pass, then merge the PR into `dev`.

### Step 8: Promote `dev` to `main`

Open the release PR that brings every change since the last release — including the version bump — into `main`:

```powershell
gh pr create --base main --head dev `
    --title "Release v1.0.0-beta.3" `
    --body "Promote dev to main for v1.0.0-beta.3 release. See docs/RELEASE_NOTES.md for the changelog."
```

Wait for CI to pass, then merge the PR. **Do not squash** this merge — preserving the individual commits keeps the release history readable and matches the previous `dev → main` merges (#126, #129).

### Step 9: Tag the release

After the `dev → main` PR is merged, sync `main` locally and tag the merge commit:

```powershell
git checkout main
git pull
git tag -a v1.0.0-beta.3 -m "v1.0.0-beta.3"
git push origin v1.0.0-beta.3
```

The tag name is the version string prefixed with `v` (matching `v1.0.0-beta.1`, `v1.0.0-beta.2`, `v0.1.0-alpha`).

### Step 10: Build the release artifacts

Build both the APK (for GitHub) and the AAB (for Play Store) from the tagged commit:

```powershell
./gradlew :composeApp:assembleRelease
./gradlew :composeApp:bundleRelease
```

Outputs:

| Artifact | Location |
|----------|----------|
| Signed APK | `composeApp/build/outputs/apk/release/composeApp-release.apk` |
| Signed AAB | `composeApp/build/outputs/bundle/release/composeApp-release.aab` |
| R8 mapping file | `composeApp/build/outputs/mapping/release/mapping.txt` |

### Step 11: Verify the APK signature

Before uploading anywhere, confirm the APK is signed with the release key (not the debug key):

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --print-certs `
    composeApp\build\outputs\apk\release\composeApp-release.apk
```

The output should show the certificate fingerprint of your **release** key. If it shows the debug key (`Android Debug, O=Android, C=US`), the signing config is not wired up — return to the Prerequisites and check `local.properties` and the `signingConfigs.release` block.

Also smoke-test the APK on a physical device or emulator before publishing:

```powershell
adb install -r composeApp\build\outputs\apk\release\composeApp-release.apk
```

Launch the app, unlock with a passphrase, and verify the core flows work (logging a period, viewing the calendar, viewing insights). R8 minification can silently break reflection-dependent code paths that pass debug builds — this smoke test is the only catch-all.

### Step 12: Publish to GitHub Releases

Create the GitHub Release and attach the APK in one command. The `#` syntax renames the uploaded asset so users see `RhythmWise-1.0.0-beta.3.apk` instead of `composeApp-release.apk`:

```powershell
gh release create v1.0.0-beta.3 `
    "composeApp\build\outputs\apk\release\composeApp-release.apk#RhythmWise-1.0.0-beta.3.apk" `
    --title "v1.0.0-beta.3" `
    --notes-file docs/RELEASE_NOTES.md `
    --prerelease
```

Flags:

- `--prerelease` — set for any `-alpha`, `-beta`, or `-rc` version. Omit for stable releases.
- `--notes-file docs/RELEASE_NOTES.md` — uses the curated narrative notes. If you would also like GitHub's auto-generated "What's Changed" PR list appended, run `gh release edit v1.0.0-beta.3 --generate-notes` afterwards.
- `--draft` — add this flag if you want to review the release page before publishing, then click "Publish release" in the GitHub UI.

After the release is published, verify by visiting `https://github.com/<owner>/<repo>/releases/tag/v1.0.0-beta.3` and confirming the APK is attached and downloadable.

### Step 13: Publish to Google Play Store

1. Open [Play Console](https://play.google.com/console) → RhythmWise → **Production** (or **Open testing** for a beta release).
2. Click **Create new release**.
3. Upload `composeApp/build/outputs/bundle/release/composeApp-release.aab`.
4. Upload the deobfuscation mapping when prompted: `composeApp/build/outputs/mapping/release/mapping.txt`. This is required for readable crash reports — uploading later is allowed but inconvenient.
5. Set the **Release name** to match the tag (e.g., `v1.0.0-beta.3`).
6. Paste the curated "What's New" section from `docs/RELEASE_NOTES.md` into the **Release notes** field (max 500 characters per locale).
7. Click **Next** → review the rollout summary → **Save** → **Send for review**.

Google's review typically takes a few hours to a few days. The release is live once review completes.

### Step 14: Post-release verification

After both channels are published:

- [ ] Download the APK from the GitHub Release page on a fresh device and confirm it installs and runs.
- [ ] Once the Play Store release is live, install/update via the Play Store on a separate device and confirm the version shown in **Settings → About** matches.
- [ ] Check Play Console → **Quality → Android vitals** for any crash spikes over the next 24-48 hours.
- [ ] Archive a copy of the APK, AAB, and `mapping.txt` to offline storage (in case the build is ever needed and the GitHub release is deleted or the keystore is rotated).

---

## Troubleshooting

**APK won't install — "App not installed" on device.**
Most often a signature mismatch with a previously-installed build. Uninstall the existing app and retry. If the device previously had a debug build, the release-signed APK cannot upgrade it in place.

**`versionCode` collision in Play Console.**
Play Console rejects a `versionCode` equal to or below any previously uploaded build (even from old internal-test tracks). Bump `versionCode` and rebuild.

**Play Store rejects upload: "App not signed with upload key".**
The upload key (the one in your keystore) and the app signing key (managed by Google Play App Signing after enrollment) are different. The AAB must be signed with the **upload** key. If you rotated the keystore, follow Play Console's key reset flow before re-uploading.

**Release APK crashes immediately on launch but debug works fine.**
R8 stripped a class referenced via reflection. Common culprits: new Room entities/DAOs, new type-converter enums, new WorkManager workers. Add a keep rule to `composeApp/proguard-rules.pro` (see the *ProGuard / R8 Rules* section of `CLAUDE.md`) and rebuild.

**`git describe --tags` returns the wrong tag in Step 4.**
The command returns the most recent tag reachable from `HEAD`. If you tagged a branch other than `main` recently, run `git describe --tags --abbrev=0 main` to scope to `main`.

**`git push` fails with `403 Permission denied to <user>` even though you have admin on the repo.**
The HTTPS remote is being authenticated through Windows Credential Manager, and the cached credential is stale or has no scope for the current repo owner (this happens after repo transfers between accounts/orgs). Clearing the credential entry usually doesn't help because the credential helper re-prompts and re-caches the same stale identity. Fix permanently by switching to SSH — see [Git auth — SSH setup](#git-auth--ssh-setup) in the Prerequisites section. Do **not** try to fix this by editing the remote URL or `git config user.*` — neither controls authentication.

**`gh release create` fails with "release already exists".**
A draft or published release with that tag already exists. Either delete the existing release (`gh release delete v1.0.0-beta.3`) and retry, or edit it in place: `gh release upload v1.0.0-beta.3 <apk>#<rename>` to add the asset, and `gh release edit v1.0.0-beta.3 --notes-file docs/RELEASE_NOTES.md` to update the notes.

---

## Future: automating with GitHub Actions

The release flow is currently manual. A future `.github/workflows/release.yml` triggered on tag push (`v*`) could:

1. Check out the tagged commit.
2. Decode the signing keystore from a GitHub secret.
3. Run `assembleRelease` and `bundleRelease`.
4. Verify the APK signature with `apksigner`.
5. Attach the APK to the GitHub Release created by the tag push (or create the release in the workflow).
6. Optionally upload the AAB to Play Console via the [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) plugin.

The keystore and `local.properties` credentials would be stored as repository secrets (`KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, etc.) and decoded into the runner workspace at build time. This is a separate body of work and is not in scope for this guide.
