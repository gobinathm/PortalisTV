# Portalis TV

An open-source, privacy-first Android TV player for **your own** Stalker/Ministra-compatible IPTV portal.

> **Status: early development.** Nothing is usable yet. See the [roadmap](docs/ROADMAP.md).

## What it is
- A native, remote-friendly TV interface with smooth scrolling through Live TV, Movies, Series and the program guide.
- Playback powered by [libVLC](https://www.videolan.org/vlc/libvlc.html).
- Configurable down to the last detail, per profile: portal, device identity, firmware and user-agent, proxy, UDPXY and player tuning.
- **Private by design.** No analytics, no ads, no tracking. The app talks only to the portal you configure.

## What it is not
Portalis TV **does not provide any content, channels, playlists or portals**. You need a subscription from a provider you are legally entitled to use. Requests for portals, credentials or free channels will be closed.

## Install
There is no app-store release. You build the app and install it on your TV yourself, which takes about 15 minutes. See **[docs/INSTALL.md](docs/INSTALL.md)**.

## Documentation
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Security policy](SECURITY.md)
- [Privacy policy](PRIVACY.md)

## License
[Apache License 2.0](LICENSE). Bundled third-party components keep their own licenses (libVLC is LGPL-2.1+). See `THIRD_PARTY_NOTICES.md` once it is added.

Portalis TV is an independent project. It is not affiliated with, endorsed by or connected to any set-top-box manufacturer or middleware vendor. Device and model names appear only where portals require them for compatibility.
