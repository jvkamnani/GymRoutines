package com.noahjutz.gymroutines.data.domain

import java.util.Date

data class ExerciseProgressPoint(
    val exerciseId: Int,
    val workoutId: Int,
    val workoutEndTime: Date,
    val weight: Double?,
    val reps: Int?,
    val time: Int?,
    val distance: Double?,
)
