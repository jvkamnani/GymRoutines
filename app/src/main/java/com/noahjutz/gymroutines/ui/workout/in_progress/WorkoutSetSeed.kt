package com.noahjutz.gymroutines.ui.workout.in_progress

import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet

fun seedWorkoutSetFromPrevious(
    groupId: Int,
    previousSet: WorkoutSet?,
): WorkoutSet {
    return WorkoutSet(
        groupId = groupId,
        reps = previousSet?.reps,
        weight = previousSet?.weight,
        time = previousSet?.time,
        distance = previousSet?.distance,
        setKind = SetKinds.NORMAL,
    )
}
