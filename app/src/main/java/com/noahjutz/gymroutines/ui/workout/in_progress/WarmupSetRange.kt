package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet

data class WarmupSetRange(
    val minimum: Int,
    val maximum: Int,
)

private val warmupSetRangeRegex =
    Regex(
        pattern = "(?im)^Warm-up range:\\s*(\\d+)\\s*-\\s*(\\d+)\\s*$",
    )

fun parseWarmupSetRange(notes: String?): WarmupSetRange? {
    if (notes.isNullOrBlank()) {
        return null
    }
    val match = warmupSetRangeRegex.find(notes) ?: return null
    val minimum = match.groupValues[1].toIntOrNull() ?: return null
    val maximum = match.groupValues[2].toIntOrNull() ?: return null
    if (minimum < 0 || maximum < minimum) {
        return null
    }
    return WarmupSetRange(minimum = minimum, maximum = maximum)
}

fun reshapeSetsForWarmupTarget(
    sets: List<WorkoutSet>,
    targetWarmupCount: Int,
): List<WorkoutSet> {
    val orderedSets = sets.sortedBy { it.workoutSetId }
    val warmupSets = orderedSets.filter { it.setKind == SetKinds.WARM_UP }
    val nonWarmupSets = orderedSets.filterNot { it.setKind == SetKinds.WARM_UP }
    val safeTarget = targetWarmupCount.coerceAtLeast(0)

    val desiredWarmups =
        if (safeTarget <= warmupSets.size) {
            warmupSets.take(safeTarget)
        } else {
            val expandedWarmups = warmupSets.toMutableList()
            val template =
                warmupSets.lastOrNull() ?: WorkoutSet(
                    groupId = orderedSets.firstOrNull()?.groupId ?: 0,
                    reps = nonWarmupSets.firstOrNull()?.reps,
                    setKind = SetKinds.WARM_UP,
                )
            repeat(safeTarget - warmupSets.size) {
                val seed = expandedWarmups.lastOrNull() ?: template
                expandedWarmups.add(
                    seed.copy(
                        setKind = SetKinds.WARM_UP,
                        complete = false,
                        workoutSetId = 0,
                    ),
                )
            }
            expandedWarmups
        }

    return desiredWarmups + nonWarmupSets
}
