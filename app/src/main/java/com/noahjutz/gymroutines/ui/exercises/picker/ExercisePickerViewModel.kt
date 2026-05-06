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

package com.noahjutz.gymroutines.ui.exercises.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern

class ExercisePickerViewModel(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    targetSetGroupId: Int,
) : ViewModel() {
    private val alternativesMarkerRegex =
        Regex("^alternatives?\\s*[:\\-]?\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val _nameFilter = MutableStateFlow("")
    private val exercises = exerciseRepository.exercises
    private val _selectedExercises = MutableStateFlow(emptyList<Exercise>())
    private val _allowedExerciseIds =
        MutableStateFlow<Set<Int>?>(if (targetSetGroupId >= 0) emptySet() else null)

    init {
        if (targetSetGroupId >= 0) {
            viewModelScope.launch {
                _allowedExerciseIds.value =
                    resolveAllowedExerciseIds(
                        exerciseRepository = exerciseRepository,
                        workoutRepository = workoutRepository,
                        targetSetGroupId = targetSetGroupId,
                    )
            }
        }
    }

    fun search(name: String) {
        _nameFilter.value = name
    }

    fun addExercise(exercise: Exercise) {
        _selectedExercises.value =
            _selectedExercises.value.toMutableList().apply { add(exercise) }
    }

    fun removeExercise(exercise: Exercise) {
        _selectedExercises.value =
            _selectedExercises.value.toMutableList().apply { remove(exercise) }
    }

    fun setSingleExercise(exercise: Exercise) {
        _selectedExercises.value = listOf(exercise)
    }

    val nameFilter = _nameFilter.asStateFlow()

    val allExercises =
        combine(exercises, _nameFilter, _allowedExerciseIds) { exercises, nameFilter, allowedExerciseIds ->
            exercises.filter {
                (allowedExerciseIds == null || allowedExerciseIds.contains(it.exerciseId)) &&
                    it.name.lowercase(Locale.getDefault())
                        .contains(nameFilter.lowercase(Locale.getDefault()))
            }
        }

    private val selectedExercises = _selectedExercises.asStateFlow()

    val selectedExerciseIds = selectedExercises.map { it.map { it.exerciseId } }

    fun exercisesContains(exercise: Exercise) = selectedExercises.map { it.contains(exercise) }

    private suspend fun resolveAllowedExerciseIds(
        exerciseRepository: ExerciseRepository,
        workoutRepository: WorkoutRepository,
        targetSetGroupId: Int,
    ): Set<Int>? {
        val setGroup = workoutRepository.getSetGroup(targetSetGroupId) ?: return null
        val sourceExerciseId = setGroup.originalExerciseId ?: setGroup.exerciseId
        val sourceExercise = exerciseRepository.getExercise(sourceExerciseId) ?: return null
        val alternativeNames = parseAlternativeNames(sourceExercise.notes)
        if (alternativeNames.isEmpty()) {
            return null
        }

        val allExercises = exerciseRepository.exercises.first()
        val exercisesByName =
            allExercises.associateBy {
                normalizeExerciseLookupKey(it.name)
            }

        return alternativeNames.mapNotNull { alternativeName ->
            exercisesByName[normalizeExerciseLookupKey(alternativeName)]?.exerciseId
        }.toSet()
    }

    private fun parseAlternativeNames(notes: String): List<String> {
        val lines = notes.lines().map { it.trim() }
        val markerLineIndex = lines.indexOfFirst { line -> alternativesMarkerRegex.matches(line) }
        if (markerLineIndex < 0) return emptyList()

        val rawAlternatives = mutableListOf<String>()
        val markerMatch = alternativesMarkerRegex.matchEntire(lines[markerLineIndex])
        val inlineAlternatives = markerMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (inlineAlternatives.isNotEmpty()) {
            rawAlternatives.add(inlineAlternatives)
        }

        for (line in lines.drop(markerLineIndex + 1)) {
            if (line.isBlank()) break
            val normalizedLine =
                line
                    .replace(Regex("^[-*•]+\\s*"), "")
                    .replace(Regex("^\\d+[.)]\\s*"), "")
                    .trim()
            if (normalizedLine.isBlank()) continue
            if (normalizedLine.contains(':')) break
            rawAlternatives.add(normalizedLine)
        }

        val splitRegex = Regex("[,;/|]")
        return rawAlternatives
            .flatMap { text ->
                text.split(splitRegex).map { token -> token.trim() }
            }
            .filter { it.isNotEmpty() }
            .distinctBy { normalizeExerciseLookupKey(it) }
    }

    private fun normalizeExerciseLookupKey(name: String): String {
        val nonAlphaNumericRegex = Pattern.compile("[^\\p{L}\\p{N}]+")
        return nonAlphaNumericRegex
            .matcher(name.trim().lowercase(Locale.getDefault()))
            .replaceAll("")
    }
}
