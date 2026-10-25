# Third-party notices

Portalis TV's own source code is licensed under the [Apache License 2.0](LICENSE). Built APKs also contain the third-party components below, which keep their own licenses.

This list covers every library in the app's release runtime classpath (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`) and every native library in the APK. It was last checked in October 2026 against the license each library declares in its Maven POM, and the native libraries against their own packages. Versions change with dependency updates; the license groups below rarely do.

## libVLC (video playback) — affects the license of built APKs
- **Component:** `org.videolan.android:libvlc-all` 3.7.7, from Maven Central. Native libraries: `libvlc.so`, `libvlcjni.so`, and `libc++_shared.so` (see below).
- **License:** libVLC is published under the GNU LGPL 2.1 or later. This prebuilt package, however, also contains components licensed under the **GNU GPL**: the x264 H.264 encoder (GPL-2.0-or-later, used by VLC's stream output) and FFmpeg's libpostproc (GPL-2.0-or-later, used by VLC's post-processing filter). Portalis TV uses neither feature, but both are compiled into the library.
- **What this means:** an APK built from this repository is a combined work that includes GPL code, so **the APK as a whole is distributed under the terms of the GNU GPL version 3**. Apache-2.0 code may be combined with GPLv3 code; the source files in this repository remain under Apache-2.0.
- **Source code:** the complete corresponding source for Portalis TV is this repository. libVLC's source is at <https://code.videolan.org/videolan/vlc-android> (`libvlcjni`) and <https://code.videolan.org/videolan/vlc>, at the tag matching the version above.
- **Possible future change:** building libVLC without the GPL components would let APKs be distributed under LGPL/Apache terms only. That is tracked for the release-APK work.

## LLVM libc++ (C++ runtime, shipped with libVLC)
- **Component:** `libc++_shared.so` from the Android NDK. **License:** Apache License 2.0 with LLVM Exceptions.

## SQLCipher for Android (encrypted database)
- **Component:** `net.zetetic:sqlcipher-android` 4.19.1, native library `libsqlcipher.so`. **License:** SQLCipher Community Edition license, a BSD-style license (<https://www.zetetic.net/sqlcipher/license/>).
- **Includes:** SQLite (public domain) and LibTomCrypt (public domain, or the Unlicense / WTFPL where public domain is not recognized), compiled into the native library.

## Apache License 2.0
All other libraries in the APK are licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0):

| Libraries | Maven groups |
|---|---|
| AndroidX and Jetpack: Activity, AppCompat, Core, Lifecycle, Navigation, Room, DataStore, Startup, Profile Installer, Window, Hilt integration and their support libraries | `androidx.*` |
| Jetpack Compose and Compose for TV (`tv-material` 1.1.0) | `androidx.compose.*`, `androidx.tv` |
| Compose Multiplatform runtime pieces that Jetpack Compose depends on | `org.jetbrains.compose.*`, `org.jetbrains.androidx.*` |
| Kotlin standard library 2.4.21, kotlinx.coroutines 1.11.0, kotlinx.serialization 1.11.0, JetBrains annotations | `org.jetbrains.kotlin`, `org.jetbrains.kotlinx`, `org.jetbrains` |
| Dagger and Hilt 2.60.1 | `com.google.dagger` |
| Tink 1.24.0 (encryption keys) | `com.google.crypto.tink` |
| Gson 2.13.2, Guava `listenablefuture`, Error Prone annotations, JSR-305 annotations, Accompanist Drawable Painter | `com.google.*` |
| OkHttp 5.5.0, Okio 3.18.1 | `com.squareup.okhttp3`, `com.squareup.okio` |
| Coil 3.6.3 (images) | `io.coil-kt.coil3` |
| JSpecify annotations | `org.jspecify` |
| `javax.inject`, Jakarta Dependency Injection API 2.0.1 (its NOTICE is included in the APK as `META-INF/NOTICE.md`) | `javax.inject`, `jakarta.inject` |

## Icons
The player icons (`feature/player/src/main/res/drawable/ic_player_*.xml`) and the category and favorite icons (`core/designsystem/src/main/res/drawable/`) are from Google's Material Icons, Apache License 2.0.

## Test streams (tools/mock-portal only, not in the app)
Big Buck Bunny, (c) copyright 2008 Blender Foundation, www.bigbuckbunny.org, CC BY 3.0. Apple and Mux sample HLS streams are referenced by URL only.
