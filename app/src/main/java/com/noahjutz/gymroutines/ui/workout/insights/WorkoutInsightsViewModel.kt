/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines.ui.workout.insights

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.ExerciseProgressPoint
import com.noahjutz.gymroutines.data.domain.Workout
import com.noahjutz.gymroutines.util.formatSimple
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class ProgressMetric {
    WEIGHT,
    REPS,
    TIME,
    DISTANCE,
}

data class ExerciseProgressChart(
    val exerciseId: Int,
    val exerciseName: String,
    val metric: ProgressMetric,
    val points: List<Pair<Float, Float>>,
    val latestValueLabel: String,
)

class WorkoutInsightsViewModel(
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    preferences: DataStore<Preferences>,
) : ViewModel() {
    val workouts =
        workoutRepository.workouts.combine(preferences.data) { workouts, prefs ->
            workouts.filter {
                prefs[AppPrefs.CurrentWorkout.key] != it.workoutId
            }
        }

    val routineNames =
        workouts.map { workouts ->
            workouts.associate {
                Pair(it.workoutId, getRoutineName(it.routineId))
            }
        }

    val exerciseProgressCharts =
        combine(
            workoutRepository.getExerciseProgressPoints(),
            exerciseRepository.exercises,
            preferences.data,
        ) { points, exercises, prefs ->
            val currentWorkoutId = prefs[AppPrefs.CurrentWorkout.key] ?: -1
            buildExerciseProgressCharts(
                points = points,
                exercises = exercises,
                excludeWorkoutId = currentWorkoutId,
            )
        }

    fun delete(workout: Workout) =
        viewModelScope.launch {
            workoutRepository.delete(workout)
        }

    private suspend fun getRoutineName(routineId: Int): String {
        val routine = routineRepository.getRoutine(routineId)
        return routine?.name ?: ""
    }
}

internal fun buildExerciseProgressCharts(
    points: List<ExerciseProgressPoint>,
    exercises: List<Exercise>,
    excludeWorkoutId: Int,
): List<ExerciseProgressChart> {
    val pointsByExercise =
        points
            .asSequence()
            .filter { it.workoutId != excludeWorkoutId }
            .groupBy { it.exerciseId }

    return exercises
        .asSequence()
        .mapNotNull { exercise ->
            val metric = progressMetricForExercise(exercise) ?: return@mapNotNull null
            val exercisePoints = pointsByExercise[exercise.exerciseId].orEmpty()
            if (exercisePoints.isEmpty()) {
                return@mapNotNull null
            }

            val chartPoints =
                exercisePoints
                    .groupBy { it.workoutId }
                    .entries
                    .sortedBy { (_, sets) -> sets.first().workoutEndTime.time }
                    .mapNotNull { (_, sets) ->
                        aggregateMetricForWorkout(metric, sets)
                    }
                    .mapIndexed { index, value ->
                        index.toFloat() to value
                    }

            if (chartPoints.isEmpty()) {
                return@mapNotNull null
            }

            ExerciseProgressChart(
                exerciseId = exercise.exerciseId,
                exerciseName = exercise.name,
                metric = metric,
                points = chartPoints,
                latestValueLabel = metricValueLabel(metric, chartPoints.last().second),
            )
        }.sortedBy { it.exerciseName.lowercase() }
        .toList()
}

internal fun progressMetricForExercise(exercise: Exercise): ProgressMetric? {
    return when {
        exercise.logWeight -> ProgressMetric.WEIGHT
        exercise.logReps -> ProgressMetric.REPS
        exercise.logTime -> ProgressMetric.TIME
        exercise.logDistance -> ProgressMetric.DISTANCE
        else -> null
    }
}

private fun aggregateMetricForWorkout(
    metric: ProgressMetric,
    sets: List<ExerciseProgressPoint>,
): Float? {
    return when (metric) {
        ProgressMetric.WEIGHT -> sets.mapNotNull { it.weight?.toFloat() }.maxOrNull()
        ProgressMetric.REPS -> sets.mapNotNull { it.reps?.toFloat() }.maxOrNull()
        ProgressMetric.TIME -> sets.mapNotNull { it.time?.toFloat() }.maxOrNull()
        ProgressMetric.DISTANCE -> sets.mapNotNull { it.distance?.toFloat() }.maxOrNull()
    }
}

private fun metricValueLabel(
    metric: ProgressMetric,
    value: Float,
): String {
    return when (metric) {
        ProgressMetric.WEIGHT -> "${value.toDouble().formatSimple()} kg"
        ProgressMetric.REPS -> "${value.toInt()} reps"
        ProgressMetric.TIME -> "${value.toInt()} s"
        ProgressMetric.DISTANCE -> value.toDouble().formatSimple()
    }
}
