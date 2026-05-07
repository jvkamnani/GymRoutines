package com.noahjutz.gymroutines.ui.workout.in_progress

fun effectiveExerciseNotes(
    activeExerciseNotes: String?,
    originalExerciseNotes: String?,
    hasAlternateExercise: Boolean,
): String? {
    val active = activeExerciseNotes?.trim()?.takeIf { it.isNotEmpty() }
    val original = originalExerciseNotes?.trim()?.takeIf { it.isNotEmpty() }

    if (!hasAlternateExercise) {
        return active
    }

    if (active == null) {
        return original
    }

    if (original == null) {
        return active
    }

    val activeVideoLine = extractVideoLine(active)
    if (activeVideoLine == null) {
        return original
    }

    return replaceOrAppendVideoLine(
        notes = original,
        videoLine = activeVideoLine,
    )
}

private fun extractVideoLine(notes: String): String? =
    notes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith(VIDEO_PREFIX, ignoreCase = true) }

private fun replaceOrAppendVideoLine(
    notes: String,
    videoLine: String,
): String {
    val lines = notes.lines().toMutableList()
    val index = lines.indexOfFirst { it.trim().startsWith(VIDEO_PREFIX, ignoreCase = true) }
    if (index >= 0) {
        lines[index] = videoLine
    } else {
        lines.add(videoLine)
    }
    return lines.joinToString("\n")
}

private const val VIDEO_PREFIX = "Video:"
