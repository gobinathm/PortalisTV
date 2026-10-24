# Developer tools

Both tools run on your computer; no TV or emulator is needed.

## Mock portal (`tools/mock-portal`)
A small Stalker-compatible portal with freely licensed test streams (Big Buck Bunny, © Blender Foundation, CC BY 3.0, and Apple's public HLS examples). Use it to try the app without a provider, for development and for screenshots.

```sh
./gradlew :tools:mock-portal:run --args="--port 8088"
```
Then add a profile with the URL `http://<your computer's IP>:8088/c/` (from the Android emulator: `http://10.0.2.2:8088/c/`) and turn on **Allow insecure HTTP**.

Options:
- `--mac 00:1A:79:00:00:01`: allow only this MAC address (repeatable); other devices are refused, as an unregistered box would be.
- `--token-ttl 30`: tokens expire after 30 seconds, to exercise re-authentication.
- `--filler 500`: how many filler channels, and filler movies under On Demand, to serve (default 30), for long lists such as the scroll benchmark's.

## Portal check (`tools/portal-check`)
Connects to a portal with the app's own client and reports each step: connect, categories, channels and a stream link. Use it to check that your provider's portal works before trying the app, or to diagnose a failure.

```sh
cp portal-check.properties.example portal-check.properties   # then fill in url and mac
./gradlew :tools:portal-check:run
```
`portal-check.properties` holds your portal address and MAC address. It is gitignored; never commit it or paste it into an issue.

The console shows only counts and outcomes. The full exchange is written to `build/portal-check/capture-*.txt` with hosts, MAC address, serial, device IDs, tokens and personal account fields masked. Read it before sharing it; masking is pattern-based and a portal may return personal data in fields it doesn't know.
