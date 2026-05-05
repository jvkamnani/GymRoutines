package com.noahjutz.gymroutines.ui.exercises.picker

import com.noahjutz.gymroutines.data.ExerciseRepository
import com.noahjutz.gymroutines.data.WorkoutRepository
import com.noahjutz.gymroutines.data.domain.Exercise
import com.noahjutz.gymroutines.data.domain.WorkoutSetGroup
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `restricts alternates to exercise names in source notes`() =
        runTest {
            val sourceExerciseId = 200
            val setGroupId = 7
            val exercises =
                listOf(
                    Exercise(name = "Barbell Bench Press", exerciseId = sourceExerciseId),
                    Exercise(name = "Dumbbell Incline Press", exerciseId = 11),
                    Exercise(name = "Smith Machine Incline Press", exerciseId = 12),
                    Exercise(name = "Cable Fly", exerciseId = 13),
                )

            val exerciseRepository = mockk<ExerciseRepository>()
            val workoutRepository = mockk<WorkoutRepository>()

            every { exerciseRepository.exercises } returns MutableStateFlow(exercises)
            coEvery { workoutRepository.getSetGroup(setGroupId) } returns
                WorkoutSetGroup(
                    workoutId = 1,
                    exerciseId = sourceExerciseId,
                    position = 0,
                    id = setGroupId,
                )
            coEvery { exerciseRepository.getExercise(sourceExerciseId) } returns
                Exercise(
                    name = "Incline Bench Press",
                    notes = "Alternatives: Dumbbell Incline Press, smith machine incline press",
                    exerciseId = sourceExerciseId,
                )

            val viewModel =
                ExercisePickerViewModel(
                    exerciseRepository = exerciseRepository,
                    workoutRepository = workoutRepository,
                    targetSetGroupId = setGroupId,
                )

            advanceUntilIdle()
            val allowedExerciseNames = viewModel.allExercises.first().map { it.name }
            assertThat(allowedExerciseNames)
                .containsExactlyInAnyOrder("Dumbbell Incline Press", "Smith Machine Incline Press")

            viewModel.search("smith")
            val smithFiltered = viewModel.allExercises.first()
            assertThat(smithFiltered.map { it.name })
                .containsExactly("Smith Machine Incline Press")
        }

    @Test
    fun `falls back to full list when source exercise has no alternatives metadata`() =
        runTest {
            val sourceExerciseId = 300
            val setGroupId = 8
            val exercises =
                listOf(
                    Exercise(name = "Front Squat", exerciseId = sourceExerciseId),
                    Exercise(name = "Hack Squat", exerciseId = 21),
                    Exercise(name = "Leg Press", exerciseId = 22),
                )

            val exerciseRepository = mockk<ExerciseRepository>()
            val workoutRepository = mockk<WorkoutRepository>()

            every { exerciseRepository.exercises } returns MutableStateFlow(exercises)
            coEvery { workoutRepository.getSetGroup(setGroupId) } returns
                WorkoutSetGroup(
                    workoutId = 1,
                    exerciseId = sourceExerciseId,
                    position = 0,
                    id = setGroupId,
                )
            coEvery { exerciseRepository.getExercise(sourceExerciseId) } returns
                Exercise(
                    name = "Front Squat",
                    notes = "Keep core braced.",
                    exerciseId = sourceExerciseId,
                )

            val viewModel =
                ExercisePickerViewModel(
                    exerciseRepository = exerciseRepository,
                    workoutRepository = workoutRepository,
                    targetSetGroupId = setGroupId,
                )

            advanceUntilIdle()
            val visibleExerciseIds = viewModel.allExercises.first().map { it.exerciseId }
            assertThat(visibleExerciseIds).containsExactly(300, 21, 22)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
