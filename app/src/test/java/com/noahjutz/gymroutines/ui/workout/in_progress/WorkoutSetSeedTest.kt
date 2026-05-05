package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WorkoutSetSeedTest {
    @Test
    fun `seeds first set from previous logged values`() {
        val previous =
            WorkoutSet(
                groupId = 10,
                reps = 8,
                weight = 42.5,
                time = 90,
                distance = 1.2,
                setKind = SetKinds.DROP,
                complete = true,
            )

        val seeded = seedWorkoutSetFromPrevious(groupId = 99, previousSet = previous)

        assertThat(seeded.groupId).isEqualTo(99)
        assertThat(seeded.reps).isEqualTo(8)
        assertThat(seeded.weight).isEqualTo(42.5)
        assertThat(seeded.time).isEqualTo(90)
        assertThat(seeded.distance).isEqualTo(1.2)
        assertThat(seeded.setKind).isEqualTo(SetKinds.NORMAL)
        assertThat(seeded.complete).isFalse()
    }

    @Test
    fun `falls back to blank working set when no previous set exists`() {
        val seeded = seedWorkoutSetFromPrevious(groupId = 88, previousSet = null)

        assertThat(seeded.groupId).isEqualTo(88)
        assertThat(seeded.reps).isNull()
        assertThat(seeded.weight).isNull()
        assertThat(seeded.time).isNull()
        assertThat(seeded.distance).isNull()
        assertThat(seeded.setKind).isEqualTo(SetKinds.NORMAL)
        assertThat(seeded.complete).isFalse()
    }
}
