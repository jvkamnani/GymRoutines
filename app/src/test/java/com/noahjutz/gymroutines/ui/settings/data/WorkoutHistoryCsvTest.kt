package com.noahjutz.gymroutines.ui.settings.data

import com.noahjutz.gymroutines.data.domain.Workout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class WorkoutHistoryCsvTest {
    @Test
    fun `builds csv with header and escaped text fields`() {
        val workout =
            Workout(
                routineId = 5,
                startTime = Date(0),
                endTime = Date(1000),
                workoutId = 10,
            )

        val csv =
            buildWorkoutHistoryCsv(
                listOf(
                    WorkoutHistoryExportRow(
                        workout = workout,
                        routineName = "Push \"A\"",
                        exerciseName = "Incline Bench Press",
                        setIndex = 1,
                        setType = "working",
                        reps = 8,
                        weight = 80.0,
                        time = null,
                        distance = null,
                        complete = true,
                        supersetTag = "SS1",
                        alternateForExerciseName = "Barbell Bench Press",
                    ),
                ),
            )

        val lines = csv.trim().split("\n")
        assertThat(lines).hasSize(2)
        assertThat(lines[0])
            .isEqualTo("workout_id,start_time,end_time,routine,exercise,set_index,set_type,reps,weight,time,distance,complete,superset,alternate_for")
        assertThat(lines[1]).contains("\"Push \"\"A\"\"\"")
        assertThat(lines[1]).contains(",1,\"working\",8,80.0,,,true,\"SS1\",\"Barbell Bench Press\"")
    }

    @Test
    fun `writes empty strings for nullable text fields`() {
        val workout =
            Workout(
                routineId = 8,
                startTime = Date(2000),
                endTime = Date(3000),
                workoutId = 11,
            )

        val csv =
            buildWorkoutHistoryCsv(
                listOf(
                    WorkoutHistoryExportRow(
                        workout = workout,
                        routineName = "",
                        exerciseName = "Row",
                        setIndex = 2,
                        setType = "warm_up",
                        reps = null,
                        weight = null,
                        time = 75,
                        distance = 0.5,
                        complete = false,
                        supersetTag = null,
                        alternateForExerciseName = null,
                    ),
                ),
            )

        val line = csv.trim().split("\n")[1]
        assertThat(line).endsWith(",\"\",\"\"")
        assertThat(line).contains(",2,\"warm_up\",,,75,0.5,false,")
    }
}
