# Third-party notices

Portalis TV's own source code is licensed under the [Apache License 2.0](LICENSE). Built APKs also contain the third-party components below, which keep their own licenses. A complete per-library audit is part of the v1.0 release checklist (see the roadmap); this file grows until then.

## libVLC (video playback) — affects the license of built APKs
- **Component:** `org.videolan.android:libvlc-all` 3.7.7, from Maven Central.
- **License:** libVLC is published under the GNU LGPL 2.1 or later. This prebuilt package, however, also contains components licensed under the **GNU GPL**: the x264 H.264 encoder (GPL-2.0-or-later, used by VLC's stream output) and FFmpeg's libpostproc (GPL-2.0-or-later, used by VLC's post-processing filter). Portalis TV uses neither feature, but both are compiled into the library.
- **What this means:** an APK built from this repository is a combined work that includes GPL code, so **the APK as a whole is distributed under the terms of the GNU GPL version 3**. Apache-2.0 code may be combined with GPLv3 code; the source files in this repository remain under Apache-2.0.
- **Source code:** the complete corresponding source for Portalis TV is this repository. libVLC's source is at <https://code.videolan.org/videolan/vlc-android> (`libvlcjni`) and <https://code.videolan.org/videolan/vlc>, at the tag matching the version above.
- **Possible future change:** building libVLC without the GPL components would let APKs be distributed under LGPL/Apache terms only. That is tracked for the release-APK work.

## SQLCipher for Android (encrypted database)
- **Component:** `net.zetetic:sqlcipher-android` 4.19.1. **License:** BSD-style (Zetetic Community Edition).

## Other libraries
AndroidX, Jetpack Compose, Room, Navigation, Hilt/Dagger, Tink, OkHttp, Okio, Coil, DataStore and kotlinx libraries are licensed under the Apache License 2.0.

## Icons
The player icons (`feature/player/src/main/res/drawable/ic_player_*.xml`) and the category and favorite icons (`core/designsystem/src/main/res/drawable/`) are from Google's Material Icons, Apache License 2.0.

## Test streams (tools/mock-portal only, not in the app)
Big Buck Bunny, (c) copyright 2008 Blender Foundation, www.bigbuckbunny.org, CC BY 3.0. Apple and Mux sample HLS streams are referenced by URL only.
