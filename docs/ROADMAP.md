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
- [ ] Create/edit profiles with the full identity and firmware catalog
- [ ] Encrypted export and import

## M2 — Protocol
- [ ] Stalker client: discovery, handshake, profile, genres, channels, stream links
- [ ] `tools/mock-portal` with legal test streams
- [ ] Fixture-based tests

## M3 — Player
- [ ] libVLC playback, on-screen controls, audio and subtitle tracks, aspect ratio
- [ ] Configurable network caching and decoding; picture-in-picture; external player

## M4 — Live TV
- [ ] Categories, channel grid, zapping, favorites, now/next
- [ ] Meets the scroll performance budget

## M5 — Movies & Series
- [ ] Paged grids, detail pages, resume, continue watching

## M6 — EPG
- [ ] Timeline guide with smooth two-dimensional scrolling

## M7 — Customization
- [ ] Themes and accent, home-row editor, key mapping, parental lock

## M8 — Hardening & v1.0
- [ ] Security self-audit (OWASP MASVS L1), license audit, `THIRD_PARTY_NOTICES.md`
- [ ] Install guide verified on real TVs; v1.0.0 tag

## Later (TODO)
- [ ] **Signed release APKs** on GitHub Releases: per-ABI APKs, SHA-256 checksums, build attestations, published signing-certificate fingerprint, Obtainium support. Requires developer registration where Android developer verification is enforced.
- [ ] Optional, off-by-default "new version available" check
- [ ] Additional content sources (M3U, Xtream)
- [ ] Android TV home-screen channels
- [ ] Remote control from a phone (pairing code and TLS)
