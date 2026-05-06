package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.util.formatSimple

private val warmupWeightPercentsByCount =
    mapOf(
        1 to listOf(60),
        2 to listOf(50, 70),
        3 to listOf(45, 65, 85),
    )

data class WarmupRecommendation(
    val percentOfWorkingWeight: Int,
    val reps: Int?,
)

fun recommendedWarmup(
    warmupIndex: Int,
    warmupCount: Int,
    workingRepTarget: Int?,
): WarmupRecommendation? {
    val percents = warmupWeightPercentsByCount[warmupCount] ?: return null
    val percent = percents.getOrNull(warmupIndex) ?: return null
    val reps =
        when {
            workingRepTarget == null -> null
            warmupIndex == 0 -> workingRepTarget
            warmupCount == 2 && warmupIndex == 1 -> workingRepTarget - 2
            warmupCount == 3 && warmupIndex == 1 -> workingRepTarget - 2
            warmupCount == 3 && warmupIndex == 2 -> workingRepTarget - 4
            else -> null
        }?.coerceAtLeast(1)
    return WarmupRecommendation(
        percentOfWorkingWeight = percent,
        reps = reps,
    )
}

fun workingRepTarget(sets: List<WorkoutSet>): Int? {
    return sets
        .filter { it.setKind != SetKinds.WARM_UP }
        .mapNotNull { it.reps }
        .firstOrNull()
}

fun workingWeightReference(previousSets: List<WorkoutSet>): Double? {
    return previousSets
        .asReversed()
        .firstOrNull { it.setKind != SetKinds.WARM_UP && it.weight != null }
        ?.weight
}

fun findComparablePreviousSet(
    currentSet: WorkoutSet,
    currentSets: List<WorkoutSet>,
    previousSets: List<WorkoutSet>,
): WorkoutSet? {
    if (previousSets.isEmpty()) {
        return null
    }
    val currentKindSets = currentSets.filter { it.setKind == currentSet.setKind }
    val previousKindSets = previousSets.filter { it.setKind == currentSet.setKind }
    val currentKindIndex = currentKindSets.indexOfFirst { it.workoutSetId == currentSet.workoutSetId }

    if (currentKindIndex >= 0) {
        previousKindSets.getOrNull(currentKindIndex)?.let { return it }
        previousKindSets.lastOrNull()?.let { return it }
    }

    if (currentSet.setKind != SetKinds.WARM_UP) {
        previousSets
            .asReversed()
            .firstOrNull { it.setKind != SetKinds.WARM_UP }
            ?.let { return it }
    }

    return previousSets.asReversed().firstOrNull()
}

fun buildSetHistoryHint(previousSet: WorkoutSet): String? {
    val parts = mutableListOf<String>()
    previousSet.weight?.let { parts.add("${it.formatSimple()} kg") }
    previousSet.reps?.let { parts.add("${it} reps") }
    previousSet.time?.let { parts.add("${it}s") }
    previousSet.distance?.let { parts.add("${it.formatSimple()} dist") }
    if (parts.isEmpty()) {
        return null
    }
    return parts.joinToString(" • ")
}
