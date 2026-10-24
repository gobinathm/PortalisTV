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
 * filled by empty blocks so every part of the row can take focus.
 */
internal fun guideBlocks(programmes: List<Programme>, fromMillis: Long, toMillis: Long): List<GuideBlock> {
    val blocks = mutableListOf<GuideBlock>()
    var cursor = fromMillis
    programmes.sortedBy { it.startMillis }
        .filter { it.endMillis > fromMillis && it.startMillis < toMillis }
        .forEach { programme ->
            val start = maxOf(programme.startMillis, cursor)
            if (start > cursor) blocks += GuideBlock(null, cursor, start)
            if (programme.endMillis > start) {
                blocks += GuideBlock(programme, start, minOf(programme.endMillis, toMillis))
                cursor = minOf(programme.endMillis, toMillis)
            }
        }
    if (cursor < toMillis) blocks += GuideBlock(null, cursor, toMillis)
    return blocks
}

/** [millis] rounded down to the half hour. */
internal fun halfHourBefore(millis: Long): Long = millis - Math.floorMod(millis, GUIDE_STEP_MS)

/**
 * Where the timeline should start once [block] has focus: unchanged while the block starts on
 * screen or is still running at the left edge; otherwise moved so it starts at the block, but
 * never before [earliest] (the half hour around now).
 */
internal fun windowFor(block: GuideBlock, windowStart: Long, earliest: Long): Long {
    val visible = block.startMillis in windowStart until windowStart + GUIDE_SPAN_MS - GUIDE_STEP_MS / 2
    val runningAtEdge = block.startMillis < windowStart && block.endMillis > windowStart
    return if (visible || runningAtEdge) windowStart else maxOf(earliest, halfHourBefore(block.startMillis))
}
