# Security self-audit

_Audit of commit `57997b7`, October 2026, before v1.0. This is a self-audit by the maintainer against the [OWASP MASVS](https://mas.owasp.org/MASVS/) v2 controls at the L1 (baseline) level. It is not an external review._

**Result:** no high or medium findings. The low findings below are either accepted, with the reason given, or listed as follow-ups.

## Method
- Read the manifest, network security config, backup rules, R8 rules and CI workflows.
- Searched the source for risky APIs (WebView, custom TLS trust, non-secure random, platform logging, intents, file and content URIs) and followed every hit.
- Traced every value the portal controls (stream URLs, image URLs, response bodies, redirects) to where it is used.
- Checked the crypto parameters against current OWASP guidance.

## Findings by area

### MASVS-STORAGE — pass
- Profiles (portal URL, MAC, serial, device IDs, login, proxy credentials) are stored in a SQLCipher database whose key is wrapped by an Android Keystore key (`:core:database` `DatabaseKey`). Settings, including the parental PIN hash, are stored in a DataStore encrypted with Tink AES-256-GCM, its keysets wrapped by the Keystore (`:core:security` `KeyManager`).
- `allowBackup="false"`, and `data_extraction_rules.xml` excludes every domain from cloud backup and device transfer.
- Logs go through one entry point (`:core:common` `Log`). Messages and exception text pass through `Redactor` (MACs, tokens, credential parameters, URL user-info, and registered portal URLs). Release builds log only warnings and errors, and R8 removes verbose and debug calls.
- Portal traffic capture exists only in debug builds (`app/src/debug`); release and benchmark builds use a no-op module.
- Secret text fields use password input with autocorrect off, so keyboards don't learn them.

### MASVS-CRYPTO — pass
- All random values (database key, salts, nonces, generated MACs and device IDs) come from `SecureRandom`.
- Exports: PBKDF2-HMAC-SHA256 with 600,000 iterations (OWASP 2023), a 16-byte salt and AES-256-GCM, with the header authenticated. On import, the iteration count is bounded (1,000 to 10,000,000), so a crafted file can't make the import run for an extremely long time.
- Parental PIN: PBKDF2-HMAC-SHA256, 100,000 iterations, 16-byte salt. Its few possible values are protected by the try limit (see AUTH), not by the hash.
- No hard-coded keys, and no deprecated algorithms or modes.

### MASVS-AUTH — pass
- The app has no accounts of its own. Portal authentication (handshake token, MAC and serial) follows the portal protocol, and tokens are held only in memory.
- Parental PIN: after 5 wrong tries, entry pauses for 30 s. Turning on adult categories and, if enabled, opening Settings and profile editing ask for the PIN.

### MASVS-NETWORK — pass, with one accepted risk
- HTTPS by default. A plain-HTTP portal needs the per-profile "Allow insecure HTTP" opt-in, enforced before any request (`PortalEndpoints.parse`), for images too (`PortalImages`). Such profiles are marked "Not secure".
- Only system certificate authorities are trusted. There are no custom trust managers or hostname verifiers.
- Redirects never switch between HTTPS and HTTP (`followSslRedirects(false)`).
- **L1 (accepted):** stream URLs from the portal may be plain `http`, `rtsp`, `rtp` or `udp`, even for an HTTPS profile, because that is how most IPTV services deliver streams. Stream tokens can then be seen on the network. The network security config therefore allows cleartext; the opt-in protects the portal API, which carries the identity and credentials.

### MASVS-PLATFORM — pass, with two accepted items
- Only the launcher activity is exported by the app's own code, and it has no VIEW or deep-link intent filters. The merged release manifest also has AndroidX's startup provider and Room's invalidation service, both not exported, and AndroidX's `ProfileInstallReceiver`. That receiver is exported but guarded by `android.permission.DUMP`, which only the system and ADB shell hold.
- No WebView and no JavaScript bridge.
- Values from the portal are validated where they enter. Stream URLs must use `http`, `https`, `rtsp`, `rtp` or `udp` (`StreamUrls`), so never `file:`, `content:` or `javascript:`. Logo and poster URLs are parsed with OkHttp's `toHttpUrlOrNull`, which accepts only `http` and `https`.
- Imports and exports use the Storage Access Framework. The app requests no storage permissions.
- **L2 (accepted):** "Open in external player" passes the stream URL, which can contain a session token, to the app the user picks. This happens only when the user asks for it.
- **L3 (accepted):** PIN and password screens don't set `FLAG_SECURE`. On a TV, screenshots and screen sharing are rare and need ADB or a paired device, and the PIN dialog doesn't show the digits.

### MASVS-CODE — pass, with one follow-up
- `minSdk` 26, `targetSdk` 36. Release builds are minified with R8 and not debuggable.
- Portal responses are capped at 32 MB, backup files at 4 MB, and paged lists at 30 pages. JSON parsing never evaluates code.
- Gradle dependency verification (checksums), wrapper validation, Dependabot, CodeQL and dependency review on PRs. GitHub Actions are pinned to commit SHAs, with read-only default token permissions.
- **L4 (follow-up):** the plan also lists OSV-Scanner for scheduled vulnerability scans of the whole dependency tree. Dependabot alerts cover most of this; add OSV-Scanner when the release workflow is built.

### MASVS-RESILIENCE — out of scope
The app is open source and built by its users, so there is nothing to hide from them. Anti-tampering and obfuscation are not goals.

### MASVS-PRIVACY — pass
- No analytics, ads, crash SDKs, Firebase or Google Play Services.
- The app contacts only the portal the user entered and the stream and image URLs it returns. It makes no other network calls.
- Device IDs come from a random per-profile seed and are never derived from hardware identifiers. Permissions: network access only.

## Build types that are never distributed
The `benchmark` build type is debug-signed and profileable, and it adds an exported receiver that seeds a test profile. It exists only for `:benchmark` on a developer's own device. Release builds don't contain it.

## Not done
- A dynamic scan with MobSF or a similar tool.
- An external review. The plan recommends one before signed release APKs are published.
