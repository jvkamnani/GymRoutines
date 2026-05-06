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

package com.noahjutz.gymroutines.ui.routines.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.data.RoutineRepository
import com.noahjutz.gymroutines.data.domain.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

class RoutineListViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    data class RoutineListEntry(
        val routine: Routine,
        val displayName: String,
        val isCompleted: Boolean,
    )

    private val _nameFilter = MutableStateFlow("")
    val nameFilter = _nameFilter.asStateFlow()

    val routines: Flow<List<RoutineListEntry>> =
        repository.routines.combine(nameFilter) { routines, filter ->
            val normalizedFilter = filter.trim().lowercase(Locale.getDefault())
            val sortedEntries =
                routines
                .filter { routine -> !routine.hidden }
                .flatMap { routine ->
                    expandRoutineWeekEntries(routine)
                }.filter { entry ->
                    normalizedFilter.isEmpty() ||
                        entry.displayName.lowercase(Locale.getDefault()).contains(normalizedFilter)
                }.let(::sortRoutineEntries)

            sortedEntries.map { entry ->
                entry.copy(isCompleted = isEntryCompletedOnLanding(entry))
            }
        }

    fun setNameFilter(name: String) {
        _nameFilter.value = name
    }

    fun deleteRoutine(routineId: Int) {
        viewModelScope.launch {
            repository.getRoutine(routineId)?.let { routine ->
                repository.update(routine.copy(hidden = true))
            }
        }
    }

    fun addRoutine(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insert(Routine())
            onComplete(id)
        }
    }
}

internal fun expandRoutineWeekEntries(routine: Routine): List<RoutineListViewModel.RoutineListEntry> {
    val weekRangeRegex =
        Regex("\\((?:[Ww]eeks?)\\s*(\\d+)\\s*-\\s*(\\d+)\\)")
    val match = weekRangeRegex.find(routine.name) ?: return listOf(
        RoutineListViewModel.RoutineListEntry(
            routine = routine,
            displayName =
                routine.name.takeIf { it.isNotBlank() } ?: "",
            isCompleted = false,
        ),
    )

    val startWeek = match.groupValues[1].toIntOrNull() ?: return listOf(
        RoutineListViewModel.RoutineListEntry(
            routine = routine,
            displayName =
                routine.name.takeIf { it.isNotBlank() } ?: "",
            isCompleted = false,
        ),
    )
    val endWeek = match.groupValues[2].toIntOrNull() ?: return listOf(
        RoutineListViewModel.RoutineListEntry(
            routine = routine,
            displayName =
                routine.name.takeIf { it.isNotBlank() } ?: "",
            isCompleted = false,
        ),
    )
    if (startWeek > endWeek) return listOf(
        RoutineListViewModel.RoutineListEntry(
            routine = routine,
            displayName =
                routine.name.takeIf { it.isNotBlank() } ?: "",
            isCompleted = false,
        ),
    )

    return (startWeek..endWeek).map { week ->
        val displayName =
            routine.name.replaceRange(
                match.range,
                "(Week $week)",
            )
        RoutineListViewModel.RoutineListEntry(
            routine = routine,
            displayName = displayName,
            isCompleted = false,
        )
    }
}

internal data class EntrySortKey(
    val week: Int?,
    val dayOrder: Int,
    val weekInRangeStart: Int?,
)

private val weekRegex = Regex("\\b[Ww]eek\\s*(\\d+)\\b")
private val weekRangeRegex = Regex("\\(\\s*[Ww]eeks?\\s*(\\d+)\\s*-\\s*(\\d+)\\s*\\)")
private val upperARegex = Regex("\\bUpper\\s*A\\b", RegexOption.IGNORE_CASE)
private val lowerARegex = Regex("\\bLower\\s*A\\b", RegexOption.IGNORE_CASE)
private val upperBRegex = Regex("\\bUpper\\s*B\\b", RegexOption.IGNORE_CASE)
private val lowerBRegex = Regex("\\bLower\\s*B\\b", RegexOption.IGNORE_CASE)
private const val completedUpToWeek = 6
private const val completedUpToDayOrder = 0 // Upper A

internal fun entrySortKey(entry: RoutineListViewModel.RoutineListEntry): EntrySortKey {
    val displayName = entry.displayName
    val explicitWeek = weekRegex.find(displayName)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val rangeStart =
        weekRangeRegex.find(entry.routine.name)?.groupValues?.getOrNull(1)?.toIntOrNull()

    val dayOrder =
        when {
            upperARegex.containsMatchIn(displayName) -> 0
            lowerARegex.containsMatchIn(displayName) -> 1
            upperBRegex.containsMatchIn(displayName) -> 2
            lowerBRegex.containsMatchIn(displayName) -> 3
            else -> 99
        }

    return EntrySortKey(
        week = explicitWeek,
        dayOrder = dayOrder,
        weekInRangeStart = rangeStart,
    )
}

internal fun sortRoutineEntries(
    entries: List<RoutineListViewModel.RoutineListEntry>,
): List<RoutineListViewModel.RoutineListEntry> {
    return entries.sortedWith(
        compareBy<RoutineListViewModel.RoutineListEntry> { entrySortKey(it).week ?: Int.MAX_VALUE }
            .thenBy { entrySortKey(it).dayOrder }
            .thenBy { entrySortKey(it).weekInRangeStart ?: Int.MAX_VALUE }
            .thenBy { it.routine.routineId }
            .thenBy { it.displayName.lowercase(Locale.getDefault()) },
    )
}

internal fun isEntryCompletedOnLanding(entry: RoutineListViewModel.RoutineListEntry): Boolean {
    val key = entrySortKey(entry)
    val week = key.week ?: return false
    if (key.dayOrder == 99) return false
    return week < completedUpToWeek ||
        (week == completedUpToWeek && key.dayOrder <= completedUpToDayOrder)
}
