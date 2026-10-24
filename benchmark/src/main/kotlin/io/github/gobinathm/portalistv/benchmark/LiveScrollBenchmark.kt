package io.github.gobinathm.portalistv.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scrolls the Live TV channel list the way a remote does, one D-pad press at a time, and records
 * frame times. The budget is P95 frame time ≤ 16 ms and ≤ 1 % jank on a low-end TV.
 *
 * Needs the mock portal with a long list (`--filler 500`) reachable from the TV; its address is
 * passed as the `portalUrl` instrumentation argument. See benchmark/README.md.
 */
@RunWith(AndroidJUnit4::class)
class LiveScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private val portalUrl: String = requireNotNull(InstrumentationRegistry.getArguments().getString("portalUrl")) {
        "Pass the mock portal address: -Pandroid.testInstrumentationRunnerArguments.portalUrl=http://<ip>:8088/c/"
    }

    @Test
    fun scrollChannelList() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        // No startup mode: a cold or warm start would close the app after setup, and the presses
        // below would land on the TV's home screen instead.
        startupMode = null,
        iterations = ITERATIONS,
        setupBlock = { openAllChannels() },
    ) {
        repeat(SCROLL_PRESSES) { press { pressDPadDown() } }
        device.waitForIdle()
    }

    /**
     * Presses a key only while the app under test is on screen. A benchmark drives a real TV;
     * keys sent to the launcher would open other apps.
     */
    private fun MacrobenchmarkScope.press(key: UiDevice.() -> Boolean) {
        check(device.currentPackageName == TARGET_PACKAGE) {
            "Stopped: ${device.currentPackageName} is on screen, not the app under test"
        }
        device.key()
    }

    private fun MacrobenchmarkScope.openAllChannels() {
        device.executeShellCommand(
            "am broadcast -n $TARGET_PACKAGE/$TARGET_PACKAGE_BASE.benchmark.SeedProfileReceiver --es url $portalUrl",
        )
        // The only profile opens at start; home focuses "All channels" first when there are no favorites.
        startActivityAndWait()
        check(device.wait(Until.hasObject(By.text("All channels")), TIMEOUT_MS)) { "Home didn't load" }
        press { pressDPadCenter() }
        check(device.wait(Until.hasObject(By.textStartsWith("Filler")), TIMEOUT_MS)) { "Channels didn't load" }
        // From the category rail into the channel list.
        press { pressDPadRight() }
        device.waitForIdle()
    }

    private companion object {
        const val TARGET_PACKAGE_BASE = "io.github.gobinathm.portalistv"
        const val TARGET_PACKAGE = "$TARGET_PACKAGE_BASE.benchmark"
        const val ITERATIONS = 5
        const val SCROLL_PRESSES = 80
        const val TIMEOUT_MS = 20_000L
    }
}
