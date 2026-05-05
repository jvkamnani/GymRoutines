package com.noahjutz.gymroutines.ui.settings.data

import com.noahjutz.gymroutines.data.domain.Workout

private const val CSV_HEADER =
    "workout_id,start_time,end_time,routine,exercise,set_index,set_type,reps,weight,time,distance,complete,superset,alternate_for"

data class WorkoutHistoryExportRow(
    val workout: Workout,
    val routineName: String,
    val exerciseName: String,
    val setIndex: Int,
    val setType: String,
    val reps: Int?,
    val weight: Double?,
    val time: Int?,
    val distance: Double?,
    val complete: Boolean,
    val supersetTag: String?,
    val alternateForExerciseName: String?,
)

fun buildWorkoutHistoryCsv(rows: List<WorkoutHistoryExportRow>): String {
    val builder = StringBuilder()
    builder.append(CSV_HEADER).append('\n')

    rows.forEach { row ->
        builder
            .append(row.workout.workoutId).append(',')
            .append(csvField(row.workout.startTime.toString())).append(',')
            .append(csvField(row.workout.endTime.toString())).append(',')
            .append(csvField(row.routineName)).append(',')
            .append(csvField(row.exerciseName)).append(',')
            .append(row.setIndex).append(',')
            .append(csvField(row.setType)).append(',')
            .append(row.reps?.toString().orEmpty()).append(',')
            .append(row.weight?.toString().orEmpty()).append(',')
            .append(row.time?.toString().orEmpty()).append(',')
            .append(row.distance?.toString().orEmpty()).append(',')
            .append(row.complete).append(',')
            .append(csvField(row.supersetTag.orEmpty())).append(',')
            .append(csvField(row.alternateForExerciseName.orEmpty()))
            .append('\n')
    }

    return builder.toString()
}

private fun csvField(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}
