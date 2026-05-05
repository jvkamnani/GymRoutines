package com.noahjutz.gymroutines.ui.settings.data

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.processphoenix.ProcessPhoenix
import com.noahjutz.gymroutines.data.AppDatabase
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.SetKinds
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DataSettingsViewModel(
    private val preferences: DataStore<Preferences>,
    private val database: AppDatabase,
    private val workoutRepository: WorkoutRepository,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val application: Application,
) : ViewModel() {
    val isWorkoutInProgress =
        preferences.data.map { preferences ->
            val currentWorkoutId = preferences[AppPrefs.CurrentWorkout.key]
            currentWorkoutId != null && currentWorkoutId >= 0
        }

    fun exportDatabase(uri: Uri) {
        database.close()
        val inStream =
            application.applicationContext
                .getDatabasePath("workout_routines_database")
                .inputStream()

        val outStream =
            application.applicationContext
                .contentResolver
                .openOutputStream(uri)

        inStream.use { input ->
            outStream?.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun importDatabase(uri: Uri) {
        database.close()
        val inStream =
            application.applicationContext
                .contentResolver
                .openInputStream(uri)

        val databasePath =
            application.applicationContext
                .getDatabasePath("workout_routines_database")

        val outStream = databasePath.outputStream()

        inStream.use { input ->
            outStream.use { output ->
                input?.copyTo(output)
            }
        }
    }

    fun restartApp() = ProcessPhoenix.triggerRebirth(application.applicationContext)

    fun exportWorkoutHistoryCsv(uri: Uri) {
        viewModelScope.launch {
            val csv = buildWorkoutHistoryCsv(buildWorkoutHistoryRows())
            val outStream =
                application.applicationContext
                    .contentResolver
                    .openOutputStream(uri)

            outStream?.bufferedWriter().use { writer ->
                writer?.write(csv)
            }
        }
    }

    fun getCurrentTimeIso(): String {
        val now = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return formatter.format(now)
    }

    fun finishWorkout() {
        viewModelScope.launch {
            preferences.edit { it[AppPrefs.CurrentWorkout.key] = -1 }
        }
    }

    private suspend fun buildWorkoutHistoryRows(): List<WorkoutHistoryExportRow> {
        val workouts = workoutRepository.workouts.first()
        val rows = mutableListOf<WorkoutHistoryExportRow>()
        val exerciseNameCache = mutableMapOf<Int, String>()
        val routineNameCache = mutableMapOf<Int, String>()

        for (workout in workouts.sortedBy { it.startTime }) {
            val routineName =
                routineNameCache.getOrPut(workout.routineId) {
                    routineRepository.getRoutine(workout.routineId)?.name ?: ""
                }
            val groups = workoutRepository.getSetGroupsInWorkout(workout.workoutId).sortedBy { it.position }
            val sets = workoutRepository.getSetsInWorkout(workout.workoutId)
            for (group in groups) {
                val exerciseName =
                    exerciseNameCache.getOrPut(group.exerciseId) {
                        exerciseRepository.getExercise(group.exerciseId)?.name ?: ""
                    }
                val alternateForExerciseName =
                    group.originalExerciseId?.let { originalId ->
                        exerciseNameCache.getOrPut(originalId) {
                            exerciseRepository.getExercise(originalId)?.name ?: ""
                        }
                    }
                val groupSets = sets.filter { it.groupId == group.id }.sortedBy { it.workoutSetId }
                for ((index, set) in groupSets.withIndex()) {
                    rows +=
                        WorkoutHistoryExportRow(
                            workout = workout,
                            routineName = routineName,
                            exerciseName = exerciseName,
                            setIndex = index + 1,
                            setType = setTypeLabel(set.setKind),
                            reps = set.reps,
                            weight = set.weight,
                            time = set.time,
                            distance = set.distance,
                            complete = set.complete,
                            supersetTag = group.supersetTag,
                            alternateForExerciseName = alternateForExerciseName,
                        )
                }
            }
        }

        return rows
    }

    private fun setTypeLabel(setKind: String): String {
        return when (setKind) {
            SetKinds.WARM_UP -> "warm_up"
            SetKinds.DROP -> "drop"
            else -> "working"
        }
    }
}
