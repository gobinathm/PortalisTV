## What and why
<!-- What does this change, and why? Link the issue if there is one. -->

## How it was tested
<!-- Commands run, devices or emulators used, D-pad navigation checked. -->

## Checklist
- [ ] `./gradlew detekt lint test assembleDebug` passes locally
- [ ] Commits are signed off (`git commit -s`)
- [ ] No portal URLs, MACs, credentials or third-party content in code, fixtures or screenshots
- [ ] No code or assets copied from other apps
- [ ] New or updated dependencies: verification metadata regenerated and reason given

## Security checklist
<!-- Required when touching :core:security, :core:network or AndroidManifest.xml. Delete otherwise. -->
- [ ] No new exported components, permissions or WebViews
- [ ] Secrets are encrypted at rest and never logged (use `Log`, which redacts)
- [ ] No custom TrustManager/HostnameVerifier; HTTP only behind the explicit per-profile opt-in
- [ ] Input from the portal is validated (URL schemes, sizes) before use
