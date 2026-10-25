package io.github.gobinathm.portalistv.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code start-up and scrolling use, so it can be compiled ahead of time when the app
 * is installed instead of while the user scrolls. The result goes into app/src/main/baseline-prof.txt;
 * see benchmark/README.md.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    private val portalUrl: String = requireNotNull(InstrumentationRegistry.getArguments().getString("portalUrl")) {
        "Pass the mock portal address: -Pandroid.testInstrumentationRunnerArguments.portalUrl=http://<ip>:8088/c/"
    }

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE) {
        val journeys = BenchmarkJourneys(this, portalUrl)
        journeys.open(card = "All channels", rowCards = LIVE_ROW, waitFor = "Filler")
        journeys.scroll()
        journeys.open(card = "Everything", rowCards = ON_DEMAND_ROW, waitFor = "Filler movie")
        journeys.scroll()
    }
}
