# Contributing to Portalis TV

Thanks for helping. Please read this first; it is short.

## Ground rules
- **No content, portals or credentials.** Do not add, link or ask for playlists, portal URLs, MAC addresses or channels. Such issues and PRs are closed.
- **Clean room.** Do not copy code, resources, layouts or assets from other IPTV or set-top-box apps, decompiled or not. Compatibility facts (protocol fields, model and firmware strings) are fine when written from scratch and described in your own words.
- **No trackers.** No analytics, crash-reporting, ads or Google Play Services dependencies.
- Security issues go through the private channel in [SECURITY.md](SECURITY.md), not public issues.

## Setup
1. JDK 17 or newer (21 recommended).
2. Android SDK with `platforms;android-37.0` and `build-tools;37.0.0`, and `sdk.dir` in `local.properties` (Android Studio does this for you).
3. Check everything the CI checks:
   ```sh
   ./gradlew detekt lint test assembleDebug
   ```
4. Install a debug build on a TV or TV emulator: `./gradlew :app:installDebug`.
5. With a TV emulator or device connected, run the on-device tests (encrypted database, migrations), which CI does not run yet:
   ```sh
   ./gradlew connectedDebugAndroidTest
   ```

Without a provider, use the mock portal; to check a real portal without a TV, use the portal check. See [tools/README.md](tools/README.md).

## Project layout
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Build conventions live in `build-logic/`; modules should apply a `portalis.*` convention plugin instead of configuring Android or Kotlin directly. Versions live only in `gradle/libs.versions.toml`.

## Code style
- Kotlin official style, enforced by detekt with its ktlint formatting rules (`config/detekt/detekt.yml`). Lint warnings and Kotlin compiler warnings fail the build.
- Log only through `core.common.log.Log`, which redacts secrets. Never call `android.util.Log` directly.
- Every screen must be fully usable with a D-pad remote.
- Add unit tests for logic; protocol changes need fixture tests.

## Dependencies
Builds verify every dependency against `gradle/verification-metadata.xml` (SHA-256 checksums and PGP signatures). When you add or update a dependency, regenerate it and review the diff:
```sh
./gradlew --write-verification-metadata pgp,sha256 --export-keys --refresh-dependencies \
  detekt lint test assembleDebug assembleRelease assembleDebugAndroidTest
```
Write mode can miss artifacts that are resolved only while the configuration cache is stored. If a normal build then reports unverified artifacts, compare each against the file in its repository and add its checksum by hand.
New dependencies need a reason in the PR description and a license compatible with Apache-2.0.

## Commits and pull requests
- Small, focused PRs. Describe what changed and why, and how you tested it.
- Use clear commit messages in the imperative mood (`Add channel zapping`).
- Sign off every commit ([Developer Certificate of Origin](https://developercertificate.org/)): `git commit -s`. By signing off you certify that you wrote the change or have the right to submit it under the project license.
- PRs that touch `:core:security`, `:core:network` or `AndroidManifest.xml` must complete the security checklist in the PR template.
