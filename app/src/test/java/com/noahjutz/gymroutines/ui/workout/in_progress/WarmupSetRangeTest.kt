package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WarmupSetRangeTest {
    @Test
    fun `parses warm-up range metadata from notes`() {
        val parsed =
            parseWarmupSetRange(
                """
                Keep shoulders down.
                Warm-up range: 2-3
                RPE: 8
                """.trimIndent(),
            )

        assertThat(parsed).isEqualTo(WarmupSetRange(minimum = 2, maximum = 3))
    }

    @Test
    fun `returns null when no warm-up range metadata exists`() {
        val parsed = parseWarmupSetRange("No warm-up metadata here.")

        assertThat(parsed).isNull()
    }

    @Test
    fun `reduces warm-up rows by removing the highest warm-up slot first`() {
        val sets =
            listOf(
                WorkoutSet(groupId = 1, reps = 8, setKind = SetKinds.WARM_UP, workoutSetId = 10),
                WorkoutSet(groupId = 1, reps = 6, setKind = SetKinds.WARM_UP, workoutSetId = 11),
                WorkoutSet(groupId = 1, reps = 4, setKind = SetKinds.WARM_UP, workoutSetId = 12),
                WorkoutSet(groupId = 1, reps = 8, setKind = SetKinds.NORMAL, workoutSetId = 13),
            )

        val reshaped = reshapeSetsForWarmupTarget(sets, targetWarmupCount = 2)

        assertThat(reshaped.map { it.setKind })
            .containsExactly(
                SetKinds.WARM_UP,
                SetKinds.WARM_UP,
                SetKinds.NORMAL,
            )
        assertThat(reshaped.filter { it.setKind == SetKinds.WARM_UP }.map { it.reps })
            .containsExactly(8, 6)
    }

    @Test
    fun `expands warm-up rows while keeping working sets after warm-ups`() {
        val sets =
            listOf(
                WorkoutSet(groupId = 1, reps = 8, setKind = SetKinds.WARM_UP, workoutSetId = 21),
                WorkoutSet(groupId = 1, reps = 8, setKind = SetKinds.NORMAL, workoutSetId = 22),
                WorkoutSet(groupId = 1, reps = 6, setKind = SetKinds.NORMAL, workoutSetId = 23),
            )

        val reshaped = reshapeSetsForWarmupTarget(sets, targetWarmupCount = 3)

        assertThat(reshaped.map { it.setKind })
            .containsExactly(
                SetKinds.WARM_UP,
                SetKinds.WARM_UP,
                SetKinds.WARM_UP,
                SetKinds.NORMAL,
                SetKinds.NORMAL,
            )
        assertThat(reshaped.take(3).all { it.complete.not() }).isTrue()
    }
}
