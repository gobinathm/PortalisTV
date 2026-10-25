package io.github.gobinathm.portalistv.feature.live

import io.github.gobinathm.portalistv.core.model.Programme

/** How much of the timeline is on screen at once. */
internal const val GUIDE_SPAN_MS = 2 * 60 * 60 * 1000L

/** The timeline moves in half-hour steps, and its labels fall on them. */
internal const val GUIDE_STEP_MS = 30 * 60 * 1000L

/** One cell of a channel's row: a programme, or a stretch the guide says nothing about. */
internal data class GuideBlock(val programme: Programme?, val startMillis: Long, val endMillis: Long)

/**
 * A channel's row from [fromMillis] to [toMillis]: its programmes, clipped to the range, with gaps
 * filled by empty blocks so every part of the row can take focus. Gaps are split at the half hours,
 * so moving up or down from a long gap lands near the same time rather than mid-screen.
 */
internal fun guideBlocks(programmes: List<Programme>, fromMillis: Long, toMillis: Long): List<GuideBlock> {
    val blocks = mutableListOf<GuideBlock>()
    fun gap(from: Long, to: Long) {
        var start = from
        while (start < to) {
            val end = minOf(halfHourBefore(start) + GUIDE_STEP_MS, to)
            blocks += GuideBlock(null, start, end)
            start = end
        }
    }
    var cursor = fromMillis
    programmes.sortedBy { it.startMillis }
        .filter { it.endMillis > fromMillis && it.startMillis < toMillis }
        .forEach { programme ->
            val start = maxOf(programme.startMillis, cursor)
            if (start > cursor) gap(cursor, start)
            if (programme.endMillis > start) {
                blocks += GuideBlock(programme, start, minOf(programme.endMillis, toMillis))
                cursor = minOf(programme.endMillis, toMillis)
            }
        }
    if (cursor < toMillis) gap(cursor, toMillis)
    return blocks
}

/** [millis] rounded down to the half hour. */
internal fun halfHourBefore(millis: Long): Long = millis - Math.floorMod(millis, GUIDE_STEP_MS)

/**
 * Where the timeline should start once [block] has focus: unchanged while the block starts on
 * screen, or began earlier but still shows for at least half an hour (a long programme reached by
 * moving up or down shouldn't pull the timeline back); otherwise moved so it starts at the block,
 * but never before [earliest] (the half hour around now).
 */
internal fun windowFor(block: GuideBlock, windowStart: Long, earliest: Long): Long {
    val visible = block.startMillis in windowStart until windowStart + GUIDE_SPAN_MS - GUIDE_STEP_MS / 2
    val mostlyShown = block.startMillis < windowStart && block.endMillis - windowStart >= GUIDE_STEP_MS
    return if (visible || mostlyShown) windowStart else maxOf(earliest, halfHourBefore(block.startMillis))
}
