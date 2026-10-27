# Roadmap

Foundation first: no user-facing feature is built until security, storage, CI and the protocol test harness are in place (M0–M2).

## M0 — Foundation
- [x] Gradle setup: version catalog, `build-logic` convention plugins, dependency verification
- [x] Module skeleton (see [ARCHITECTURE.md](ARCHITECTURE.md#modules))
- [x] Hilt, design-system tokens, a D-pad navigable placeholder home
- [x] `:core:security`: Keystore + Tink key management; redacting logger in `:core:common`
- [x] CI: build, lint, detekt, unit tests, CodeQL, dependency review, Dependabot
- [x] `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, issue and PR templates
- [x] Verified on a TV emulator or device

## M1 — Profiles
- [x] Encrypted Room database (SQLCipher) with migration tests
- [x] Create/edit profiles with the full identity and firmware catalog
- [x] Encrypted export and import

## M2 — Protocol
- [x] Stalker client: discovery, handshake, profile, genres, channels, stream links
- [x] `tools/mock-portal` with legal test streams; `tools/portal-check` for real portals
- [x] Fixture-based tests, and end-to-end tests against the mock portal
- [x] Verified against a real portal; reply shapes recorded as synthetic fixtures

## M3 — Player
- [x] libVLC playback, on-screen controls, audio and subtitle tracks, aspect ratio
- [x] Configurable network caching and decoding; picture-in-picture; external player

## M4 — Live TV
- [x] Categories, channel grid, zapping, favorites, now/next
- [ ] Meets the scroll performance budget

## M5 — Movies & Series
- [x] Paged grids, detail pages, resume, continue watching
- [x] Search (titles and actors, on portals that support it) and a year filter

## M6 — EPG
- [x] Timeline guide with smooth two-dimensional scrolling
- [x] Guide-style Live TV: browse a category as a TV guide, with each channel's programmes along a timeline

## M7 — Customization
- [x] Themes and accent, home-row editor, key mapping, parental lock
- [x] Language categories written in their own script (हिन्दी, தமிழ், …) on Home and in the category rail

## M8 — Hardening & v1.0
- [ ] Performance pass, once features are done: meet the [scrolling budget](ARCHITECTURE.md#performance-budget) with `ScrollBenchmark` (see [benchmark/README.md](../benchmark/README.md)). P95 frame time on the Google TV Streamer, Oct 25 → Oct 27: channel list 29 → 19 ms, On Demand grid 34 → 24 ms; TV guide 47 ms (first measurement). Done: the backdrop is one cached texture pass, so the GPU no longer holds frames back; On Demand loads three pages per update; model classes are declared stable for Compose; the category rail no longer redraws on page loads or focus moves. Left: a poster card takes about 4 ms to compose on the Streamer, so a row of 8 that must appear in one frame (when scrolling catches up with loading) costs ~35 ms. Next: a lighter poster card, then the guide
- [x] Security self-audit (OWASP MASVS L1): [SECURITY_AUDIT.md](SECURITY_AUDIT.md)
- [x] License audit, complete [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)
- [x] In-app About screen: version, privacy summary, third-party notices
- [x] Install guide followed from a clean clone on macOS, installed on a Google TV Streamer
- [x] Release builds can play: R8 keep rules for libVLC (minified builds crashed on the first stream)
- [ ] Install guide on an older Android TV box; release build on Linux and Windows (CI job `install-guide`, needs a first run)
- [ ] v1.0.0 tag

## Later (TODO)
- [ ] **Signed release APKs** on GitHub Releases: per-ABI APKs, SHA-256 checksums, build attestations, published signing-certificate fingerprint, Obtainium support. Requires developer registration where Android developer verification is enforced.
- [ ] Optional, off-by-default "new version available" check
- [ ] Additional content sources (M3U, Xtream)
- [x] Channel logos for your own build, matched by name in the iptv-org database at build time (`tools/logo-pack`); never committed, optional per build
- [ ] Android TV home-screen channels
- [ ] Remote control from a phone (pairing code and TLS)
- [x] **Tablets and touch** (landscape, regular tablet sizes): tap and long-press on every screen, tap and swipe in the player, hints that say "tap" when used by touch. Portrait and phone-size layouts are not planned
