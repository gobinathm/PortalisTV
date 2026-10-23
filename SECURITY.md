# Security Policy

Security is a primary goal of Portalis TV. Thank you for helping keep users safe.

## Reporting a vulnerability
**Do not open a public issue.** Report privately via **GitHub → Security → [Report a vulnerability](../../security/advisories/new)**.

Please include the affected version or commit, the steps to reproduce, and the impact. We aim to acknowledge reports within 7 days and to agree on a disclosure timeline with you.

## Scope
In scope: the app source in this repository, its build configuration and its CI workflows.
Out of scope: third-party portals and streams, and vulnerabilities in upstream libraries that are already reported upstream (tell us anyway if they affect Portalis TV).

## Design commitments
- Secrets (portal URLs, MAC, serial, device IDs, tokens, PINs) are encrypted at rest with keys held by the Android Keystore.
- HTTPS by default. Plain HTTP portals require an explicit per-profile opt-in and are marked "Not secure".
- No embedded web portal and no JavaScript bridge.
- Only the launcher activity is exported.
- Secrets are redacted from all logs.
- Dependencies are pinned and verified. CI actions are pinned to commit SHAs.

Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#security).
