# Benchmarks

Macrobenchmarks for the performance budget in [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md). They need a real TV (or a TV emulator) over ADB and the mock portal on your computer; nothing here runs in CI.

1. Start the mock portal with a long channel list:
   ```sh
   ./gradlew :tools:mock-portal:run --args="--port 8088 --filler 500"
   ```
2. With the TV connected over ADB (only one connection to it in `adb devices`), run:
   ```sh
   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.portalUrl=http://<your computer's IP>:8088/c/
   ```
   This installs the app's `benchmark` build (release code, debug signature, package `io.github.gobinathm.portalistv.benchmark`) next to your normal install, and replaces that build's profiles with one for the mock portal.
3. Results are in `benchmark/build/outputs/connected_android_test_additional_output/`.

`ScrollBenchmark` opens All channels (Live TV) and Everything (On Demand) and presses Down 80 times per iteration in each. Look at `frameDurationCpuMs` P95 (budget: ≤ 16 ms) and `frameOverrunMs`.
