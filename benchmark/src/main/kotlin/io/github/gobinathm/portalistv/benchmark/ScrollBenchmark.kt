package io.github.gobinathm.portalistv.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scrolls the long lists the way a remote does, one D-pad press at a time, and records frame
 * times. The budget is P95 frame time ≤ 16 ms and ≤ 1 % jank on a low-end TV.
 *
 * Needs the mock portal with long lists (`--filler 500`) reachable from the TV; its address is
 * passed as the `portalUrl` instrumentation argument. See benchmark/README.md.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private val portalUrl: String = requireNotNull(InstrumentationRegistry.getArguments().getString("portalUrl")) {
        "Pass the mock portal address: -Pandroid.testInstrumentationRunnerArguments.portalUrl=http://<ip>:8088/c/"
    }

    /** Live TV's channel list, all 500+ channels. */
    @Test
    fun scrollChannelList() = measureScroll(openCard = "All channels", rowCards = LIVE_ROW, waitFor = "Filler")

    /** On Demand's poster grid, all 500+ titles: the budget's reference case. */
    @Test
    fun scrollOnDemandGrid() =
        measureScroll(openCard = "Everything", rowCards = ON_DEMAND_ROW, waitFor = "Filler movie")

    /** The TV guide for All channels: each row lays out a timeline of programmes. */
    @Test
    fun scrollGuide() = measureScroll(openCard = "All channels", rowCards = LIVE_ROW, waitFor = "Filler") {
        openGuide(railEntry = "All channels")
    }

    private fun measureScroll(
        openCard: String,
        rowCards: Set<String>,
        waitFor: String,
        thenOpen: BenchmarkJourneys.() -> Unit = {},
    ) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        // No startup mode: a cold or warm start would close the app after setup, and the presses
        // below would land on the TV's home screen instead.
        startupMode = null,
        iterations = ITERATIONS,
        setupBlock = { BenchmarkJourneys(this, portalUrl).apply { open(openCard, rowCards, waitFor) }.thenOpen() },
    ) {
        BenchmarkJourneys(this, portalUrl).scroll()
    }

    private companion object {
        const val ITERATIONS = 5
    }
}
