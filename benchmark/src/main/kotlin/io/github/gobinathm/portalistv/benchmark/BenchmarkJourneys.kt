package io.github.gobinathm.portalistv.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE_BASE = "io.github.gobinathm.portalistv"
internal const val TARGET_PACKAGE = "$TARGET_PACKAGE_BASE.benchmark"

/** The mock portal's cards in Home's Live TV and On Demand rows; no name is in both. */
internal val LIVE_ROW = setOf("All channels", "Test streams", "Movies", "Filler")
internal val ON_DEMAND_ROW = setOf("Everything", "Films", "Series", "Filler movies")

/** The steps the benchmarks and the Baseline Profile take through the app, with remote keys only. */
internal class BenchmarkJourneys(private val scope: MacrobenchmarkScope, private val portalUrl: String) {
    private val device get() = scope.device

    /** Scrolls the open list the way a remote does, one press at a time. */
    fun scroll() {
        repeat(SCROLL_PRESSES) { press { pressDPadDown() } }
        device.waitForIdle()
    }

    /**
     * Seeds the mock portal profile, opens Home, picks [card] (the first card of the row listing
     * [rowCards]) and moves into the list it opens.
     */
    fun open(card: String, rowCards: Set<String>, waitFor: String) {
        // Without a startup mode the app isn't restarted between iterations, and would reopen
        // where the last one left it; start each from Home.
        scope.killProcess()
        device.executeShellCommand(
            "am broadcast -n $TARGET_PACKAGE/$TARGET_PACKAGE_BASE.benchmark.SeedProfileReceiver --es url $portalUrl",
        )
        // The only profile opens at start, with focus on the first card of its first row.
        scope.startActivityAndWait()
        check(device.wait(Until.hasObject(By.text("Live TV")), TIMEOUT_MS)) { "Home didn't load: ${screenText()}" }
        // Home places its first focus once its rows have loaded; pressing before that moves it away.
        check(device.wait(Until.hasObject(By.focused(true)), TIMEOUT_MS)) { "Nothing took focus on Home" }
        device.waitForIdle()
        // Remote keys only: a touch would put the TV in touch mode, where the first key press is lost.
        var moves = 0
        while (!focusedHas(card)) {
            check(moves++ < MAX_ROW_MOVES) { "Couldn't reach \"$card\" on Home: ${screenText()}" }
            // Down to the card's row, which focus may enter at any card, then left to its start.
            val inRow = rowCards.any(::focusedHas)
            press { if (inRow) pressDPadLeft() else pressDPadDown() }
            device.waitForIdle()
        }
        press { pressDPadCenter() }
        check(device.wait(Until.hasObject(By.textStartsWith(waitFor)), TIMEOUT_MS)) { "\"$waitFor\" didn't load" }
        // From the category rail into the list.
        press { pressDPadRight() }
        device.waitForIdle()
    }

    /**
     * From where [open] left focus to the Guide button, and into the guide. On a slow TV the move
     * into the list can land before the list takes focus, leaving focus on [railEntry]; from
     * there it is Right, from the list Up.
     */
    fun openGuide(railEntry: String) {
        var moves = 0
        while (!focusedHas(GUIDE_BUTTON)) {
            check(moves++ < MAX_ROW_MOVES) { "Couldn't reach the Guide button: ${screenText()}" }
            val onRail = focusedHas(railEntry)
            press { if (onRail) pressDPadRight() else pressDPadUp() }
            device.waitForIdle()
        }
        press { pressDPadCenter() }
        check(device.wait(Until.hasObject(By.textEndsWith("· $GUIDE_BUTTON")), TIMEOUT_MS)) { "The guide didn't open" }
        // The guide focuses its first channel once the channels are in.
        check(device.wait(Until.hasObject(By.focused(true)), TIMEOUT_MS)) { "Nothing took focus in the guide" }
        device.waitForIdle()
    }

    /** What the screen shows, for failure messages. */
    private fun screenText(): String =
        device.currentPackageName + ": " +
            device.findObjects(By.textContains("")).mapNotNull { it.text }.joinToString(" | ")

    /** Whether the focused element (a card) shows [text]; the text sits in a child of the focused node. */
    private fun focusedHas(text: String): Boolean {
        val focused = device.findObject(By.focused(true)) ?: return false
        // Focus can move on between finding the node and reading it; callers ask again.
        return try {
            focused.text == text || focused.hasObject(By.text(text))
        } catch (_: StaleObjectException) {
            false
        }
    }

    /**
     * Presses a key only while the app under test is on screen. A benchmark drives a real TV;
     * keys sent to the launcher would open other apps.
     */
    private fun press(key: UiDevice.() -> Boolean) {
        checkInApp()
        device.key()
    }

    private fun checkInApp() = check(device.currentPackageName == TARGET_PACKAGE) {
        "Stopped: ${device.currentPackageName} is on screen, not the app under test"
    }

    private companion object {
        const val SCROLL_PRESSES = 80
        const val TIMEOUT_MS = 20_000L
        const val MAX_ROW_MOVES = 10
        const val GUIDE_BUTTON = "Guide"
    }
}
