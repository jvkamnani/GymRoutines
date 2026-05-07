package com.noahjutz.gymroutines.ui.routines.list

import com.noahjutz.gymroutines.data.domain.Routine
import com.noahjutz.gymroutines.data.domain.Workout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

class RoutineListViewModelTest {
    @Test
    fun `expands week ranges into individual opening-page entries`() {
        val routine =
            Routine(
                routineId = 10,
                name = "Essentials 4x - Upper A (Weeks 1-4)",
            )

        val entries = expandRoutineWeekEntries(routine)

        assertThat(entries.map { it.displayName })
            .containsExactly(
                "Essentials 4x - Upper A (Week 1)",
                "Essentials 4x - Upper A (Week 2)",
                "Essentials 4x - Upper A (Week 3)",
                "Essentials 4x - Upper A (Week 4)",
            )
        assertThat(entries.map { it.routine.routineId }).containsOnly(10)
        assertThat(entries.map { it.isCompleted }).containsOnly(false)
    }

    @Test
    fun `keeps non-ranged routine names unchanged`() {
        val routine =
            Routine(
                routineId = 11,
                name = "Push Day",
            )

        val entries = expandRoutineWeekEntries(routine)

        assertThat(entries).hasSize(1)
        assertThat(entries.single().displayName).isEqualTo("Push Day")
        assertThat(entries.single().routine.routineId).isEqualTo(11)
        assertThat(entries.single().isCompleted).isFalse()
    }

    @Test
    fun `sorts expanded entries by week then workout day order`() {
        val input =
            listOf(
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 4, name = "Essentials 4x - Lower B (Weeks 1-4)"),
                    displayName = "Essentials 4x - Lower B (Week 3)",
                    isCompleted = false,
                ),
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 3, name = "Essentials 4x - Upper B (Weeks 1-4)"),
                    displayName = "Essentials 4x - Upper B (Week 1)",
                    isCompleted = false,
                ),
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 2, name = "Essentials 4x - Lower A (Weeks 1-4)"),
                    displayName = "Essentials 4x - Lower A (Week 1)",
                    isCompleted = false,
                ),
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 1, name = "Essentials 4x - Upper A (Weeks 1-4)"),
                    displayName = "Essentials 4x - Upper A (Week 2)",
                    isCompleted = false,
                ),
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 1, name = "Essentials 4x - Upper A (Weeks 1-4)"),
                    displayName = "Essentials 4x - Upper A (Week 1)",
                    isCompleted = false,
                ),
                RoutineListViewModel.RoutineListEntry(
                    routine = Routine(routineId = 3, name = "Essentials 4x - Upper B (Weeks 1-4)"),
                    displayName = "Essentials 4x - Upper B (Week 2)",
                    isCompleted = false,
                ),
            )

        val sorted = sortRoutineEntries(input)

        assertThat(sorted.map { it.displayName })
            .containsExactly(
                "Essentials 4x - Upper A (Week 1)",
                "Essentials 4x - Lower A (Week 1)",
                "Essentials 4x - Upper B (Week 1)",
                "Essentials 4x - Upper A (Week 2)",
                "Essentials 4x - Upper B (Week 2)",
                "Essentials 4x - Lower B (Week 3)",
            )
    }

    @Test
    fun `marks entries completed through week 6 upper a on landing`() {
        val completedEntry =
            RoutineListViewModel.RoutineListEntry(
                routine = Routine(routineId = 20, name = "Essentials 4x - Upper A (Weeks 5-8)"),
                displayName = "Essentials 4x - Upper A (Week 6)",
                isCompleted = false,
            )
        val completedEntryWithoutParentheses =
            RoutineListViewModel.RoutineListEntry(
                routine = Routine(routineId = 22, name = "Essentials 4x - Upper A (Weeks 5-8)"),
                displayName = "Essentials 4x - Upper A Week 6",
                isCompleted = false,
            )
        val pendingEntry =
            RoutineListViewModel.RoutineListEntry(
                routine = Routine(routineId = 21, name = "Essentials 4x - Lower A (Weeks 5-8)"),
                displayName = "Essentials 4x - Lower A (Week 6)",
                isCompleted = false,
            )

        assertThat(isEntryCompletedOnLanding(completedEntry)).isTrue()
        assertThat(isEntryCompletedOnLanding(completedEntryWithoutParentheses)).isTrue()
        assertThat(isEntryCompletedOnLanding(pendingEntry)).isFalse()
    }

    @Test
    fun `advances completion by one entry for each finished plan workout`() {
        val upperA =
            Routine(routineId = 1, name = "Essentials 4x - Upper A (Weeks 1-12)")
        val lowerA =
            Routine(routineId = 2, name = "Essentials 4x - Lower A (Weeks 1-12)")
        val upperB =
            Routine(routineId = 3, name = "Essentials 4x - Upper B (Weeks 1-12)")
        val lowerB =
            Routine(routineId = 4, name = "Essentials 4x - Lower B (Weeks 1-12)")

        val entries =
            sortRoutineEntries(
                listOf(upperA, lowerA, upperB, lowerB)
                    .flatMap(::expandRoutineWeekEntries),
            )

        val progressed =
            applyCompletionProgress(
                entries = entries,
                workouts =
                    listOf(
                        Workout(
                            routineId = 2,
                            workoutId = 999,
                            startTime = Date(1_000),
                            endTime = Date(2_000),
                        ),
                    ),
                currentWorkoutId = -1,
            )

        val week6UpperAIndex = entries.indexOfFirst { it.displayName.contains("Upper A (Week 6)") }
        val week6LowerAIndex = entries.indexOfFirst { it.displayName.contains("Lower A (Week 6)") }

        assertThat(week6UpperAIndex).isGreaterThanOrEqualTo(0)
        assertThat(week6LowerAIndex).isEqualTo(week6UpperAIndex + 1)
        assertThat(progressed[week6UpperAIndex].isCompleted).isTrue()
        assertThat(progressed[week6LowerAIndex].isCompleted).isTrue()
    }

    @Test
    fun `does not advance completion for the active in progress workout`() {
        val upperA =
            Routine(routineId = 1, name = "Essentials 4x - Upper A (Weeks 1-12)")
        val lowerA =
            Routine(routineId = 2, name = "Essentials 4x - Lower A (Weeks 1-12)")
        val upperB =
            Routine(routineId = 3, name = "Essentials 4x - Upper B (Weeks 1-12)")
        val lowerB =
            Routine(routineId = 4, name = "Essentials 4x - Lower B (Weeks 1-12)")

        val entries =
            sortRoutineEntries(
                listOf(upperA, lowerA, upperB, lowerB)
                    .flatMap(::expandRoutineWeekEntries),
            )

        val progressed =
            applyCompletionProgress(
                entries = entries,
                workouts =
                    listOf(
                        Workout(
                            routineId = 2,
                            workoutId = 555,
                            startTime = Date(1_000),
                            endTime = Date(2_000),
                        ),
                    ),
                currentWorkoutId = 555,
            )

        val week6LowerAIndex = entries.indexOfFirst { it.displayName.contains("Lower A (Week 6)") }
        assertThat(week6LowerAIndex).isGreaterThanOrEqualTo(0)
        assertThat(progressed[week6LowerAIndex].isCompleted).isFalse()
    }
}
