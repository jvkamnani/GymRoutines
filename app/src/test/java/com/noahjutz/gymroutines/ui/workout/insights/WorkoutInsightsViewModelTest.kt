package com.noahjutz.gymroutines.ui.workout.insights

import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.ExerciseProgressPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class WorkoutInsightsViewModelTest {
    @Test
    fun `build charts uses weight metric and excludes current workout`() {
        val exercise =
            Exercise(
                name = "Barbell Bench Press",
                logReps = true,
                logWeight = true,
                exerciseId = 7,
            )
        val points =
            listOf(
                ExerciseProgressPoint(
                    exerciseId = 7,
                    workoutId = 1,
                    workoutEndTime = Date(1_000),
                    weight = 60.0,
                    reps = 8,
                    time = null,
                    distance = null,
                ),
                ExerciseProgressPoint(
                    exerciseId = 7,
                    workoutId = 1,
                    workoutEndTime = Date(1_000),
                    weight = 65.0,
                    reps = 6,
                    time = null,
                    distance = null,
                ),
                ExerciseProgressPoint(
                    exerciseId = 7,
                    workoutId = 2,
                    workoutEndTime = Date(2_000),
                    weight = 70.0,
                    reps = 5,
                    time = null,
                    distance = null,
                ),
                ExerciseProgressPoint(
                    exerciseId = 7,
                    workoutId = 3,
                    workoutEndTime = Date(3_000),
                    weight = 80.0,
                    reps = 4,
                    time = null,
                    distance = null,
                ),
            )

        val charts =
            buildExerciseProgressCharts(
                points = points,
                exercises = listOf(exercise),
                excludeWorkoutId = 3,
            )

        assertThat(charts).hasSize(1)
        assertThat(charts.first().metric).isEqualTo(ProgressMetric.WEIGHT)
        assertThat(charts.first().points).containsExactly(0f to 65f, 1f to 70f)
        assertThat(charts.first().latestValueLabel).isEqualTo("70 kg")
    }

    @Test
    fun `build charts falls back to reps metric when weight is disabled`() {
        val exercise =
            Exercise(
                name = "Plank",
                logReps = true,
                logWeight = false,
                logTime = false,
                logDistance = false,
                exerciseId = 11,
            )
        val points =
            listOf(
                ExerciseProgressPoint(
                    exerciseId = 11,
                    workoutId = 1,
                    workoutEndTime = Date(1_000),
                    weight = null,
                    reps = 20,
                    time = null,
                    distance = null,
                ),
                ExerciseProgressPoint(
                    exerciseId = 11,
                    workoutId = 2,
                    workoutEndTime = Date(2_000),
                    weight = null,
                    reps = 24,
                    time = null,
                    distance = null,
                ),
            )

        val charts =
            buildExerciseProgressCharts(
                points = points,
                exercises = listOf(exercise),
                excludeWorkoutId = -1,
            )

        assertThat(charts).hasSize(1)
        assertThat(charts.first().metric).isEqualTo(ProgressMetric.REPS)
        assertThat(charts.first().points).containsExactly(0f to 20f, 1f to 24f)
        assertThat(charts.first().latestValueLabel).isEqualTo("24 reps")
    }
}
