package com.noahjutz.gymroutines.ui.workout.in_progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseNotesTest {
    @Test
    fun effectiveExerciseNotes_returnsActiveNotesWhenPresent() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = "Rest: 3 min",
                originalExerciseNotes = "Rest: 2 min",
                hasAlternateExercise = false,
            )

        assertEquals("Rest: 3 min", actual)
    }

    @Test
    fun effectiveExerciseNotes_fallsBackToOriginalNotesWhenAlternateHasNoNotes() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = "   ",
                originalExerciseNotes = "RPE: 8\nRest: 3 min",
                hasAlternateExercise = true,
            )

        assertEquals("RPE: 8\nRest: 3 min", actual)
    }

    @Test
    fun effectiveExerciseNotes_usesOriginalNotesButSwapsVideoFromActiveAlternate() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = "Video: https://youtu.be/alt",
                originalExerciseNotes = "RPE: 8\nRest: 3 min\nVideo: https://youtu.be/original",
                hasAlternateExercise = true,
            )

        assertEquals(
            "RPE: 8\nRest: 3 min\nVideo: https://youtu.be/alt",
            actual,
        )
    }

    @Test
    fun effectiveExerciseNotes_appendsActiveAlternateVideoWhenOriginalHasNoVideoLine() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = "Video: https://youtu.be/alt",
                originalExerciseNotes = "RPE: 8\nRest: 3 min",
                hasAlternateExercise = true,
            )

        assertEquals(
            "RPE: 8\nRest: 3 min\nVideo: https://youtu.be/alt",
            actual,
        )
    }

    @Test
    fun effectiveExerciseNotes_keepsOriginalWhenActiveAlternateHasNoVideoLine() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = "Cue: elbows tucked",
                originalExerciseNotes = "RPE: 8\nRest: 3 min\nVideo: https://youtu.be/original",
                hasAlternateExercise = true,
            )

        assertEquals(
            "RPE: 8\nRest: 3 min\nVideo: https://youtu.be/original",
            actual,
        )
    }

    @Test
    fun effectiveExerciseNotes_returnsNullWhenNoNotesAvailable() {
        val actual =
            effectiveExerciseNotes(
                activeExerciseNotes = null,
                originalExerciseNotes = "RPE: 8",
                hasAlternateExercise = false,
            )

        assertNull(actual)
    }
}
