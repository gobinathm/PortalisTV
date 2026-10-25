# Build and install Portalis TV

Portalis TV is not on any app store. You build it on your computer and install it on your TV over the network with ADB. ADB installs work on all Android TV / Google TV devices and are not affected by Android's developer-verification requirements for sideloaded apps.

## 1. Prepare your computer (one time)
1. Install **JDK 17 or newer** and make sure `java -version` and `keytool` work in your terminal.
   - macOS: `brew install openjdk@21`. Homebrew does not put it on your `PATH`, so add this to `~/.zshrc` and open a new terminal:
     ```sh
     export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
     export PATH="$JAVA_HOME/bin:$PATH"
     ```
   - Windows/Linux: [Adoptium Temurin](https://adoptium.net/) (tick "Set JAVA_HOME" in the Windows installer).
2. Install the **Android SDK**.
   - **With [Android Studio](https://developer.android.com/studio):** open the project once; Studio installs what it needs and writes `local.properties`. Skip to step 3.
   - **macOS without Android Studio:**
     ```sh
     brew install --cask android-commandlinetools
     export ANDROID_HOME="$(brew --prefix)/share/android-commandlinetools"   # also add this to ~/.zshrc
     yes | sdkmanager --licenses
     sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
     ```
   Gradle finds the SDK through `ANDROID_HOME`, or through a `local.properties` file in the project folder containing `sdk.dir=/path/to/sdk`.
3. Make sure `adb` is on your `PATH` (`adb version` should print a version). Android Studio and the command-line tools put it in `<sdk>/platform-tools`.

**Windows:** run the commands below in **Git Bash** (installed with [Git for Windows](https://git-scm.com/download/win)) so the signing script works, and use `./gradlew` as shown.

## 2. Build
```sh
git clone https://github.com/gobinathm/PortalisTV.git
cd PortalisTV
./scripts/create-signing-key.sh     # one time: creates YOUR private signing key (kept out of git)
./gradlew :app:assembleRelease
```
The first build downloads Gradle and the libraries and takes several minutes; later builds are much faster. The APKs appear in `app/build/outputs/apk/release/`: `app-arm64-v8a-release.apk` for 64-bit TVs, `app-armeabi-v7a-release.apk` for 32-bit ones, and `app-universal-release.apk`, which works on both but is larger. Many current TVs run 32-bit apps only even on 64-bit chips (the Google TV Streamer is one), so if you are unsure, check with `adb shell getprop ro.product.cpu.abilist` or use the universal APK.

If the file names end in `-unsigned.apk` instead, the signing key is missing: run `./scripts/create-signing-key.sh` and build again. Unsigned APKs cannot be installed.

**Back up the signing key** (`keystore.properties` and `portalis-release.jks`). Updates must be signed with the same key. If you lose it, you have to uninstall the app before installing a new build, and your profiles are lost unless you exported a backup.

## 3. Enable developer mode on the TV (one time)
1. **Settings → System → About** (on some TVs: *Device Preferences → About*).
2. Select **Android TV OS build** (or **Build**) **7 times** until it says you are a developer.
3. Go back to **Settings → System → Developer options** and turn on:
   - **USB debugging**, and
   - **Wireless debugging** (Google TV / Android 11+) or **Network debugging** (older devices).
4. Note the TV's IP address (**Settings → Network**).

## 4. Connect and install
**Google TV / Android 11+ (wireless debugging):**
```sh
# On the TV: Wireless debugging → "Pair device with pairing code"
adb pair <tv-ip>:<pairing-port>      # enter the 6-digit code shown on the TV
adb devices                          # newer TVs often connect by themselves after pairing
adb connect <tv-ip>:<port>           # only if the TV isn't listed; port shown on the Wireless debugging screen
```
Pairing is needed once per computer. Later, `adb connect` is enough, and the port changes whenever the TV restarts; `adb mdns services` shows the current one.
**Older Android TV (network debugging):**
```sh
adb connect <tv-ip>:5555             # accept the prompt on the TV
```
**Install:**
```sh
./gradlew :app:installRelease
# or
adb install -r app/build/outputs/apk/release/app-universal-release.apk
```
Portalis TV now appears in your TV's app list.

## 5. Update
```sh
git pull
./gradlew :app:installRelease
```
Your profiles and settings are kept, as long as you sign with the same key.

## 6. Turn developer mode off (optional, recommended)
After installing, you can turn **USB/Wireless debugging** off again. The app keeps working.

## 7. Back up and move profiles (optional)
On the profile screen, **Back up profiles** writes all profiles to a file encrypted with a password you choose, and **Restore from backup** reads one back. Profiles with the same identity are replaced; others are added. The password can't be recovered, so keep it somewhere safe.

On TVs with a file manager, the app asks where to save or open the file. Most TVs, including Google TV, have none; the app then uses its own folder, which you reach with ADB (debugging must be on):
```sh
# Copy backups from the TV to your computer
adb pull /sdcard/Android/data/io.github.gobinathm.portalistv/files/backups/ .
# Copy a backup to another TV, then choose "Restore from backup" there
adb push portalis-profiles-….ptvb /sdcard/Android/data/io.github.gobinathm.portalistv/files/backups/
```
This folder is deleted when the app is uninstalled, so copy backups off the TV before uninstalling. Debug builds use `io.github.gobinathm.portalistv.debug` in the path.

## 8. Channel logos for your build (optional)
Many portals send no channel logos, so the app shows each channel's name on a colour instead. You can pack real logos into **your own** build: a tool on your computer reads your channel list, looks each channel up by name in the public [iptv-org](https://github.com/iptv-org/database) database, and writes small PNGs to `local-logos/`. The next build includes them; the TV still talks only to your portal.

1. Copy `portal-check.properties.example` to `portal-check.properties` and fill in your portal address and MAC (the file is git-ignored).
2. Make the logos (this takes a few minutes the first time):
   ```sh
   ./gradlew :tools:logo-pack:run
   # or only some categories, by words in their names:
   ./gradlew :tools:logo-pack:run --args="--only=HINDI,TAMIL"
   ```
   It prints how many channels matched and the size; `build/logo-pack/report.tsv` lists what each channel matched.
3. Build and install as usual (step 4). To build **without** the logos, add `-Pportalis.logos=false` (or put `portalis.logos=false` in `gradle.properties`), or delete `local-logos/`.

Run it again when your provider adds channels. Matching is by name, so some channels won't get a logo, and occasionally one gets a wrong one. `local-logos/` is git-ignored: logos are their owners' trademarks, so never commit or share them.

## Troubleshooting
| Problem | Fix |
|---|---|
| `SDK location not found` | Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=…` (step 1.2) |
| `Unable to locate a Java Runtime` / `keytool: command not found` | The JDK is not on your `PATH`; see step 1.1 |
| `Failed to find target with hash string 'android-37'` | Run `sdkmanager "platforms;android-37.0" "build-tools;37.0.0"` |
| `adb connect` fails | TV and computer must be on the same network; re-check the IP and port; re-enable debugging |
| `adb: more than one device/emulator` | The TV is connected twice (e.g. automatically and with `adb connect`). Run `adb devices`, then `adb disconnect <tv-ip>:<port>` for the extra entry |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The installed build was signed with a different key. Export a backup, uninstall, then install. |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Your TV's CPU isn't supported by this build; open an issue with `adb shell getprop ro.product.cpu.abilist` |
| App not on the home screen | Look under **Apps → See all apps** |
