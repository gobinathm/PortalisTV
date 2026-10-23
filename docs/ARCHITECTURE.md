# Architecture

## Goals
1. **Modern:** Kotlin, Jetpack Compose for TV, coroutines and Flow, and a clean modular structure.
2. **Customizable:** every portal and device option is configurable per profile, along with themes, home layout, player tuning and key mapping.
3. **Secure:** encrypted at rest, HTTPS by default, a minimal attack surface and a verified supply chain.
4. **Private:** no telemetry; data goes only to the user's own portal.
5. **Smooth:** native lists and grids with a measured performance budget.

## Stack
| Concern | Choice |
|---|---|
| Language | Kotlin (K2), JVM target 17 |
| UI | Jetpack Compose + `androidx.tv:tv-material` (Material 3 for TV) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Networking | OkHttp + kotlinx.serialization |
| Database | Room on SQLCipher (encrypted) |
| Settings | Proto DataStore, encrypted with Tink |
| Keys | Android Keystore wrapping a Tink AEAD keyset |
| Images | Coil 3 |
| Paging | Paging 3 |
| Player | libVLC behind a `Player` interface |
| Build | Gradle Kotlin DSL, version catalog, convention plugins in `build-logic/`, Gradle dependency verification |
| Quality | Android Lint (warnings as errors), detekt, ktlint, unit tests, Compose UI tests, Macrobenchmark + Baseline Profiles |

**SDK levels:** `minSdk 26`, `targetSdk 36`, `compileSdk 37` (required by current AndroidX). **ABIs:** `arm64-v8a`, `armeabi-v7a`.
**applicationId:** `io.github.gobinathm.portalistv`.

## Modules
```
:app                   entry point, navigation, DI setup
:core:model            pure-Kotlin domain models
:core:common           dispatchers, result types, redacting logger
:core:security         Keystore/Tink key management, PIN hashing, redaction
:core:catalog          device catalog: STB models, firmware images, user agents (JSON)
:core:database         Room + SQLCipher, DAOs, migrations
:core:datastore        encrypted app settings
:core:network          OkHttp factory (per-profile proxy, user-agent, HTTP policy)
:core:stalker          Stalker/Ministra protocol client (pure Kotlin)
:core:data             repositories
:core:player           Player interface and track/aspect models
:player:vlc            libVLC implementation
:core:designsystem     theme tokens, typography, focus effects, components
:feature:onboarding  :feature:profiles  :feature:home  :feature:live
:feature:vod         :feature:series    :feature:epg   :feature:player
:feature:settings
:benchmark             Macrobenchmark + Baseline Profile generator
tools/mock-portal      local test portal with legal test streams
tools/portal-check     command-line check of a real portal with the app's client
```
Modules are added when the milestone that needs them starts; M0 shipped `:app`, `:core:common`, `:core:security`, `:core:designsystem` and `:feature:home`; M1 adds `:core:model`, `:core:catalog`, `:core:database`, `:core:data` and `:feature:profiles`; M2 adds `:core:network`, `:core:stalker`, `tools/mock-portal` and `tools/portal-check`.

**Dependency rule:** `feature → core:data → core:{stalker, database, …} → core:model`. Feature modules never depend on each other, and only `:app` wires navigation.

## Content sources
Screens consume a `ContentSource` interface (categories, channels, movies, series, EPG, stream links). `:core:stalker` is the first implementation. Other source types can be added later without touching the UI.

### Stalker client
- **Portal discovery:** probe the common portal endpoints under the configured base URL.
- **Session:** a handshake returns a token; the profile request sends the device identity. Requests carry the bearer token, a set-top-box user-agent, and cookies for MAC, language and timezone. The client re-authenticates automatically and sends keep-alives during playback.
- **Robustness:** strict timeouts, a response size cap, tolerant JSON parsing, retries only for idempotent reads, and secrets never logged.
- **Tests:** MockWebServer replaying recorded, redacted fixtures.

## Configuration model
- **Profile (per portal):** name; portal URL and language; timezone; plain-HTTP opt-in; proxy; STB model; firmware preset or custom (versions, image info); user-agent preset or custom; display resolution; MAC; serial; device ID / ID2 / signature; hardware vendor/version; compatibility toggles; player choice; UDPXY; external-player key events; favorites; hidden and locked categories.
- **App settings:** theme, accent, text scale, home rows, language; VLC tuning (network caching, hardware decoding, deinterlace, aspect, time-stretch, preferred audio and subtitle languages, subtitle style); controls (overlay timeout, key mapping, number zapping); picture-in-picture; background behavior; start on boot; media session; settings and parental PINs; encrypted backup and restore.

## Security
| Asset | Threat | Control |
|---|---|---|
| Portal credentials | Theft from storage or backup | SQLCipher DB, key wrapped by Android Keystore; excluded from cloud backup; exports only password-encrypted |
| Credentials in transit | Interception | HTTPS default; HTTP only with explicit per-profile opt-in and a "Not secure" badge; no custom trust managers; optional trust-on-first-use certificate pinning |
| App integrity | Malicious portal responses | No WebView and no JS bridge; stream URL scheme allow-list; response size limits |
| Device | Abuse by other apps | Only the launcher activity exported; boot receiver opt-in and not exported |
| Privacy | Leaks through logs | Redacting logger; verbose logs stripped from release builds |
| Locks | PIN brute force | Salted slow hash; exponential back-off |
| Supply chain | Compromised dependency or CI | Gradle dependency verification; wrapper validation; actions pinned to SHAs; least-privilege tokens; Dependabot, CodeQL, OSV-Scanner |

## Performance budget
- Scrolling a 500-item grid on a low-end Android TV device: **P95 frame time ≤ 16 ms, jank ≤ 1 %**.
- Cold start to home with cached data: **≤ 1.5 s**.

Both are enforced with Macrobenchmark. Baseline Profiles cover startup, home and grid scrolling.

## Design language
Full-bleed backdrops with soft scrims, large typography, generous spacing and a single accent color. Focused items scale gently and glow instead of getting a hard outline. Animations are springy and quick (200–300 ms). Player controls are minimal and translucent.
