package com.noahjutz.gymroutines.ui.workout.in_progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerTest {
    @Test
    fun parseRestDurationSeconds_parsesMinutes() {
        val notes = "RPE: 8\nRest: ~3 min\nVideo: example"

        val actual = parseRestDurationSeconds(notes)

        assertEquals(180, actual)
    }

    @Test
    fun parseRestDurationSeconds_parsesMinuteRangeUpperBound() {
        val notes = "Rest: 2-3 min"

        val actual = parseRestDurationSeconds(notes)

        assertEquals(180, actual)
    }

    @Test
    fun parseRestDurationSeconds_parsesSecondValue() {
        val notes = "Rest: 90 sec"

        val actual = parseRestDurationSeconds(notes)

        assertEquals(90, actual)
    }

    @Test
    fun parseRestDurationSeconds_parsesMinuteAndSeconds() {
        val notes = "Rest: 1 min 30 sec"

        val actual = parseRestDurationSeconds(notes)

        assertEquals(90, actual)
    }

    @Test
    fun parseRestDurationSeconds_parsesClockStyle() {
        val notes = "Rest: 1:45"

        val actual = parseRestDurationSeconds(notes)

        assertEquals(105, actual)
    }

    @Test
    fun parseRestDurationSeconds_returnsNullWhenMissingRestLine() {
        val notes = "RPE: 8\nVideo: example"

        val actual = parseRestDurationSeconds(notes)

        assertNull(actual)
    }

    @Test
    fun formatRestDuration_formatsMinutesAndSeconds() {
        assertEquals("2:05", formatRestDuration(125))
    }
}
