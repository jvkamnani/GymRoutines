package com.noahjutz.gymroutines.ui.workout.in_progress

import java.util.Locale

private val restLineRegex = Regex("""(?im)^\s*rest\s*:\s*(.+)\s*$""")
private val clockTimeRegex = Regex("""(\d{1,2})\s*:\s*(\d{1,2})""")
private val minuteAndSecondRegex = Regex("""(\d+)\s*(?:minutes?|mins?|min|m)\s*(\d+)\s*(?:seconds?|secs?|sec|s)\b""")
private val rangeWithUnitRegex = Regex("""(\d+)\s*-\s*(\d+)\s*(minutes?|mins?|min|m|seconds?|secs?|sec|s)\b""")
private val singleWithUnitRegex = Regex("""(\d+)\s*(minutes?|mins?|min|m|seconds?|secs?|sec|s)\b""")

fun parseRestDurationSeconds(notes: String?): Int? {
    val restLine = restLineRegex.find(notes.orEmpty())?.groupValues?.getOrNull(1)?.trim() ?: return null
    val lowerRestLine = restLine.lowercase(Locale.ROOT)

    clockTimeRegex.find(restLine)?.let { match ->
        val minutes = match.groupValues[1].toIntOrNull() ?: return@let
        val seconds = match.groupValues[2].toIntOrNull() ?: return@let
        return minutes * 60 + seconds
    }

    minuteAndSecondRegex.find(lowerRestLine)?.let { match ->
        val minutes = match.groupValues[1].toIntOrNull() ?: return@let
        val seconds = match.groupValues[2].toIntOrNull() ?: return@let
        return minutes * 60 + seconds
    }

    rangeWithUnitRegex.find(lowerRestLine)?.let { match ->
        val upperBound = match.groupValues[2].toIntOrNull() ?: return@let
        return if (match.groupValues[3].startsWith("s")) upperBound else upperBound * 60
    }

    singleWithUnitRegex.find(lowerRestLine)?.let { match ->
        val value = match.groupValues[1].toIntOrNull() ?: return@let
        return if (match.groupValues[2].startsWith("s")) value else value * 60
    }

    return null
}

fun formatRestDuration(seconds: Int): String {
    val boundedSeconds = seconds.coerceAtLeast(0)
    val minutes = boundedSeconds / 60
    val remainderSeconds = boundedSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, remainderSeconds)
}
