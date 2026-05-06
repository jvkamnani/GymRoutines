package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WorkoutSetGuidanceTest {
    @Test
    fun `recommends 50 and 70 percent for two warm-up sets with reduced second reps`() {
        val first = recommendedWarmup(warmupIndex = 0, warmupCount = 2, workingRepTarget = 12)
        val second = recommendedWarmup(warmupIndex = 1, warmupCount = 2, workingRepTarget = 12)

        assertThat(first).isEqualTo(WarmupRecommendation(percentOfWorkingWeight = 50, reps = 12))
        assertThat(second).isEqualTo(WarmupRecommendation(percentOfWorkingWeight = 70, reps = 10))
    }

    @Test
    fun `recommends full pyramid for three warm-up sets`() {
        val first = recommendedWarmup(warmupIndex = 0, warmupCount = 3, workingRepTarget = 10)
        val second = recommendedWarmup(warmupIndex = 1, warmupCount = 3, workingRepTarget = 10)
        val third = recommendedWarmup(warmupIndex = 2, warmupCount = 3, workingRepTarget = 10)

        assertThat(first?.percentOfWorkingWeight).isEqualTo(45)
        assertThat(second?.percentOfWorkingWeight).isEqualTo(65)
        assertThat(third?.percentOfWorkingWeight).isEqualTo(85)
        assertThat(second?.reps).isEqualTo(8)
        assertThat(third?.reps).isEqualTo(6)
    }

    @Test
    fun `finds same kind and index from previous sets`() {
        val currentSets =
            listOf(
                WorkoutSet(groupId = 1, setKind = SetKinds.WARM_UP, workoutSetId = 10),
                WorkoutSet(groupId = 1, setKind = SetKinds.NORMAL, workoutSetId = 11),
                WorkoutSet(groupId = 1, setKind = SetKinds.NORMAL, workoutSetId = 12),
            )
        val previousSets =
            listOf(
                WorkoutSet(groupId = 9, setKind = SetKinds.WARM_UP, weight = 35.0, workoutSetId = 20),
                WorkoutSet(groupId = 9, setKind = SetKinds.NORMAL, weight = 70.0, reps = 8, workoutSetId = 21),
                WorkoutSet(groupId = 9, setKind = SetKinds.NORMAL, weight = 72.5, reps = 8, workoutSetId = 22),
            )

        val comparable =
            findComparablePreviousSet(
                currentSet = currentSets[2],
                currentSets = currentSets,
                previousSets = previousSets,
            )

        assertThat(comparable?.weight).isEqualTo(72.5)
        assertThat(buildSetHistoryHint(comparable!!)).isEqualTo("72.5 kg • 8 reps")
    }
}
