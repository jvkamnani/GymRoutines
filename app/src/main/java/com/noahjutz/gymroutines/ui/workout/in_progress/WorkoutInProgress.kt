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

package com.noahjutz.gymroutines.ui.workout.in_progress

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutSet
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.ui.components.AutoSelectTextField
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.durationVisualTransformation
import com.noahjutz.gymroutines.util.RegexPatterns
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.pretty
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun WorkoutInProgress(
    navToExercisePicker: () -> Unit,
    navToAlternateExercisePicker: (Int) -> Unit,
    navToWorkoutCompleted: (Int, Int) -> Unit,
    popBackStack: () -> Unit,
    workoutId: Int,
    exerciseIdsToAdd: List<Int>,
    alternateExerciseSelection: String?,
    viewModel: WorkoutInProgressViewModel = getViewModel { parametersOf(workoutId) },
) {
    LaunchedEffect(exerciseIdsToAdd, alternateExerciseSelection) {
        if (alternateExerciseSelection != null) {
            val parts = alternateExerciseSelection.split(":")
            val setGroupId = parts.getOrNull(0)?.toIntOrNull()
            val exerciseId = parts.getOrNull(1)?.toIntOrNull()
            if (setGroupId != null && exerciseId != null) {
                viewModel.setAlternateExercise(setGroupId, exerciseId)
            }
        } else if (exerciseIdsToAdd.isNotEmpty()) {
            viewModel.addExercises(exerciseIdsToAdd)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_perform_workout),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val workout by viewModel.workout.collectAsState(initial = null)

        Crossfade(workout == null, Modifier.padding(paddingValues)) { isNull ->
            if (isNull) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                workout?.let { workout ->
                    WorkoutInProgressContent(
                        workout = workout,
                        viewModel = viewModel,
                        popBackStack = popBackStack,
                        navToExercisePicker = navToExercisePicker,
                        navToAlternateExercisePicker = navToAlternateExercisePicker,
                        navToWorkoutCompleted = navToWorkoutCompleted,
                    )
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
)
@Composable
private fun WorkoutInProgressContent(
    workout: WorkoutWithSetGroups,
    viewModel: WorkoutInProgressViewModel,
    popBackStack: () -> Unit,
    navToExercisePicker: () -> Unit,
    navToAlternateExercisePicker: (Int) -> Unit,
    navToWorkoutCompleted: (Int, Int) -> Unit,
) {
    var showFinishWorkoutDialog by remember { mutableStateOf(false) }
    var pendingDeleteSet by remember { mutableStateOf<WorkoutSet?>(null) }
    var showDeleteSetFinalConfirmation by remember { mutableStateOf(false) }
    if (showFinishWorkoutDialog) {
        FinishWorkoutDialog(
            onDismiss = { showFinishWorkoutDialog = false },
            finishWorkout = {
                viewModel.finishWorkout {
                    navToWorkoutCompleted(workout.workout.workoutId, workout.workout.routineId)
                }
            },
        )
    }
    if (pendingDeleteSet != null && !showDeleteSetFinalConfirmation) {
        ConfirmDeleteSetDialog(
            onDismiss = { pendingDeleteSet = null },
            onContinue = { showDeleteSetFinalConfirmation = true },
        )
    }
    if (pendingDeleteSet != null && showDeleteSetFinalConfirmation) {
        ConfirmDeleteSetFinalDialog(
            onDismiss = {
                pendingDeleteSet = null
                showDeleteSetFinalConfirmation = false
            },
            onConfirm = {
                pendingDeleteSet?.let(viewModel::deleteSet)
                pendingDeleteSet = null
                showDeleteSetFinalConfirmation = false
            },
        )
    }

    var showCancelWorkoutDialog by remember { mutableStateOf(false) }
    if (showCancelWorkoutDialog) {
        CancelWorkoutDialog(
            onDismiss = { showCancelWorkoutDialog = false },
            cancelWorkout = {
                viewModel.cancelWorkout(popBackStack)
            },
        )
    }

    LazyColumn(Modifier.fillMaxHeight()) {
        item {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp),
                color = colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp),
            ) {
                val routineName by viewModel.routineName.collectAsState("")
                Text(
                    text = routineName,
                    modifier = Modifier.padding(24.dp),
                    style = typography.headlineSmall,
                )
            }
            Text(
                workout.workout.duration.pretty(),
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp),
                style = typography.headlineSmall.copy(textAlign = TextAlign.Center),
            )
        }

        items(workout.setGroups.sortedBy { it.group.position }, key = { it.group.id }) { setGroup ->
            val exercise by viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)
            val originalExercise by viewModel.getExercise(setGroup.group.originalExerciseId ?: -1)
                .collectAsState(initial = null)
            val previousSets by produceState(
                initialValue = emptyList(),
                key1 = setGroup.group.exerciseId,
                key2 = workout.workout.workoutId,
            ) {
                value = viewModel.getMostRecentSetsForExercise(setGroup.group.exerciseId)
            }
            val orderedSets = remember(setGroup.sets) { setGroup.sets.sortedBy { it.workoutSetId } }
            val warmupRange = parseWarmupSetRange(exercise?.notes)
            val currentWarmupCount = orderedSets.count { it.setKind == SetKinds.WARM_UP }
            val repTarget = workingRepTarget(orderedSets)
            val referenceWorkingWeight = workingWeightReference(previousSets)
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            ) {
                Column {
                    Surface(Modifier.fillMaxWidth(), color = colorScheme.primary) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                exercise?.name.toString(),
                                style = typography.headlineSmall,
                                modifier =
                                    Modifier
                                        .padding(16.dp)
                                        .weight(1f),
                            )

                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    modifier = Modifier.padding(16.dp),
                                    onClick = { expanded = !expanded },
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        stringResource(R.string.drag_handle),
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            val id = setGroup.group.id
                                            val toId =
                                                workout.setGroups
                                                    .find { it.group.position == setGroup.group.position - 1 }
                                                    ?.group
                                                    ?.id
                                            if (toId != null) {
                                                viewModel.swapSetGroups(id, toId)
                                            }
                                        },
                                        text = {
                                            Text(stringResource(R.string.btn_move_up))
                                        },
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            val id = setGroup.group.id
                                            val toId =
                                                workout.setGroups
                                                    .find { it.group.position == setGroup.group.position + 1 }
                                                    ?.group
                                                    ?.id
                                            if (toId != null) {
                                                viewModel.swapSetGroups(id, toId)
                                            }
                                        },
                                        text = {
                                            Text(stringResource(R.string.btn_move_down))
                                        },
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            navToAlternateExercisePicker(setGroup.group.id)
                                        },
                                        text = {
                                            Text(stringResource(R.string.btn_log_alternate_exercise))
                                        },
                                    )
                                    if (setGroup.group.originalExerciseId != null) {
                                        DropdownMenuItem(
                                            onClick = {
                                                expanded = false
                                                viewModel.clearAlternateExercise(setGroup.group.id)
                                            },
                                            text = {
                                                Text(stringResource(R.string.btn_clear_alternate_exercise))
                                            },
                                        )
                                    }
                                    if (warmupRange != null && currentWarmupCount > warmupRange.minimum) {
                                        DropdownMenuItem(
                                            onClick = {
                                                expanded = false
                                                viewModel.setWarmupSetCount(
                                                    setGroup = setGroup,
                                                    targetWarmupCount = warmupRange.minimum,
                                                )
                                            },
                                            text = {
                                                Text(stringResource(R.string.btn_use_minimum_warm_up))
                                            },
                                        )
                                    }
                                    if (warmupRange != null && currentWarmupCount < warmupRange.maximum) {
                                        DropdownMenuItem(
                                            onClick = {
                                                expanded = false
                                                viewModel.setWarmupSetCount(
                                                    setGroup = setGroup,
                                                    targetWarmupCount = warmupRange.maximum,
                                                )
                                            },
                                            text = {
                                                Text(stringResource(R.string.btn_use_full_warm_up))
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            viewModel.pairSupersetWithPrevious(setGroup.group.id)
                                        },
                                        enabled = setGroup.group.position > 0,
                                        text = {
                                            Text(stringResource(R.string.btn_pair_superset_with_previous))
                                        },
                                    )
                                    if (setGroup.group.supersetTag != null) {
                                        DropdownMenuItem(
                                            onClick = {
                                                expanded = false
                                                viewModel.clearSuperset(setGroup.group.id)
                                            },
                                            text = {
                                                Text(stringResource(R.string.btn_clear_superset))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (setGroup.group.originalExerciseId != null || setGroup.group.supersetTag != null) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            if (setGroup.group.originalExerciseId != null) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.label_alternate_for,
                                            originalExercise?.name ?: "",
                                        ),
                                    style = typography.bodyMedium,
                                )
                            }
                            if (setGroup.group.supersetTag != null) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.label_superset_tag,
                                            setGroup.group.supersetTag ?: "",
                                        ),
                                    style = typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (exercise?.notes?.isNotBlank() == true) {
                        Text(
                            text = exercise?.notes ?: "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = typography.bodyMedium,
                        )
                    }
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.padding(horizontal = 4.dp)) {
                            val headerTextStyle =
                                TextStyle(
                                    color = colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .width(110.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.column_set_type),
                                    style = headerTextStyle,
                                )
                            }
                            if (exercise?.logReps == true) {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.column_reps),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise?.logWeight == true) {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.column_weight),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise?.logTime == true) {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.column_time),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise?.logDistance == true) {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.column_distance),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    stringResource(R.string.column_set_complete),
                                )
                            }
                            Box(
                                Modifier
                                    .padding(4.dp)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    stringResource(R.string.btn_delete),
                                )
                            }
                        }
                        for (set in orderedSets) {
                            key(set.workoutSetId) {
                                val warmupIndex =
                                    orderedSets
                                        .filter { it.setKind == SetKinds.WARM_UP }
                                        .indexOfFirst { it.workoutSetId == set.workoutSetId }
                                val warmupRecommendation =
                                    if (set.setKind == SetKinds.WARM_UP && warmupIndex >= 0) {
                                        recommendedWarmup(
                                            warmupIndex = warmupIndex,
                                            warmupCount = currentWarmupCount,
                                            workingRepTarget = repTarget,
                                        )
                                    } else {
                                        null
                                    }
                                val previousSet =
                                    findComparablePreviousSet(
                                        currentSet = set,
                                        currentSets = orderedSets,
                                        previousSets = previousSets,
                                    )
                                val previousSetText = previousSet?.let(::buildSetHistoryHint)
                                Surface {
                                    Column {
                                        Row(
                                            Modifier.padding(horizontal = 4.dp),
                                        ) {
                                            val textFieldStyle =
                                                typography.bodyMedium.copy(
                                                    textAlign = TextAlign.Center,
                                                    color = colorScheme.onSurface,
                                                )
                                            val decorationBox: @Composable (@Composable () -> Unit) -> Unit =
                                                { innerTextField ->
                                                    Surface(
                                                        color = colorScheme.onSurface.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                    ) {
                                                        Box(
                                                            Modifier
                                                                .padding(horizontal = 4.dp)
                                                                .height(56.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            innerTextField()
                                                        }
                                                    }
                                                }
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .padding(4.dp)
                                                        .width(110.dp),
                                            ) {
                                                var typeMenuExpanded by remember { mutableStateOf(false) }
                                                Surface(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(56.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { typeMenuExpanded = true },
                                                    color = colorScheme.onSurface.copy(alpha = 0.1f),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(stringResource(setKindLabelRes(set.setKind)))
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = typeMenuExpanded,
                                                    onDismissRequest = { typeMenuExpanded = false },
                                                ) {
                                                    SetKinds.all.forEach { setKind ->
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                typeMenuExpanded = false
                                                                viewModel.updateSetKind(set, setKind)
                                                            },
                                                            text = {
                                                                Text(stringResource(setKindLabelRes(setKind)))
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                            if (exercise?.logReps == true) {
                                                val (reps, setReps) =
                                                    remember {
                                                        mutableStateOf(
                                                            set.reps.toStringOrBlank(),
                                                        )
                                                    }
                                                LaunchedEffect(reps) {
                                                    val repsInt = reps.toIntOrNull()
                                                    viewModel.updateReps(set, repsInt)
                                                }
                                                AutoSelectTextField(
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(4.dp),
                                                    value = reps,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.integer)) {
                                                            setReps(it)
                                                        }
                                                    },
                                                    textStyle = textFieldStyle,
                                                    keyboardOptions =
                                                        KeyboardOptions(
                                                            keyboardType = KeyboardType.Number,
                                                        ),
                                                    singleLine = true,
                                                    cursorColor = colorScheme.onSurface,
                                                    decorationBox = decorationBox,
                                                )
                                            }
                                            if (exercise?.logWeight == true) {
                                                val (weight, setWeight) =
                                                    remember {
                                                        mutableStateOf(
                                                            set.weight.formatSimple(),
                                                        )
                                                    }
                                                LaunchedEffect(weight) {
                                                    val weightDouble = weight.toDoubleOrNull()
                                                    viewModel.updateWeight(set, weightDouble)
                                                }
                                                AutoSelectTextField(
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(4.dp),
                                                    value = weight,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.float)) {
                                                            setWeight(it)
                                                        }
                                                    },
                                                    keyboardOptions =
                                                        KeyboardOptions(
                                                            keyboardType = KeyboardType.Number,
                                                        ),
                                                    singleLine = true,
                                                    textStyle = textFieldStyle,
                                                    cursorColor = colorScheme.onSurface,
                                                    decorationBox = decorationBox,
                                                )
                                            }
                                            if (exercise?.logTime == true) {
                                                val (time, setTime) =
                                                    remember {
                                                        mutableStateOf(
                                                            set.time.toStringOrBlank(),
                                                        )
                                                    }
                                                LaunchedEffect(time) {
                                                    val timeInt = time.toIntOrNull()
                                                    viewModel.updateTime(set, timeInt)
                                                }
                                                AutoSelectTextField(
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(4.dp),
                                                    value = time,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.duration)) {
                                                            setTime(it)
                                                        }
                                                    },
                                                    keyboardOptions =
                                                        KeyboardOptions(
                                                            keyboardType = KeyboardType.Number,
                                                        ),
                                                    singleLine = true,
                                                    textStyle = textFieldStyle,
                                                    visualTransformation = durationVisualTransformation,
                                                    cursorColor = colorScheme.onSurface,
                                                    decorationBox = decorationBox,
                                                )
                                            }
                                            if (exercise?.logDistance == true) {
                                                val (distance, setDistance) =
                                                    remember {
                                                        mutableStateOf(
                                                            set.distance.formatSimple(),
                                                        )
                                                    }
                                                LaunchedEffect(distance) {
                                                    val distanceDouble = distance.toDoubleOrNull()
                                                    viewModel.updateDistance(set, distanceDouble)
                                                }
                                                AutoSelectTextField(
                                                    modifier =
                                                        Modifier
                                                            .weight(1f)
                                                            .padding(4.dp),
                                                    value = distance,
                                                    onValueChange = {
                                                        if (it.matches(RegexPatterns.float)) {
                                                            setDistance(it)
                                                        }
                                                    },
                                                    keyboardOptions =
                                                        KeyboardOptions(
                                                            keyboardType = KeyboardType.Number,
                                                        ),
                                                    singleLine = true,
                                                    textStyle = textFieldStyle,
                                                    cursorColor = colorScheme.onSurface,
                                                    decorationBox = decorationBox,
                                                )
                                            }
                                            Box(
                                                Modifier
                                                    .padding(4.dp)
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .toggleable(
                                                        value = set.complete,
                                                        onValueChange = {
                                                            viewModel.updateChecked(set, it)
                                                        },
                                                    )
                                                    .background(
                                                        animateColorAsState(
                                                            if (set.complete) {
                                                                colorScheme.secondary
                                                            } else {
                                                                colorScheme.onSurface.copy(
                                                                    alpha = 0.1f,
                                                                )
                                                            },
                                                        ).value,
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = set.complete,
                                                    enter = fadeIn(),
                                                    exit = fadeOut(),
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        stringResource(R.string.column_set_complete),
                                                        tint = colorScheme.onSecondary,
                                                    )
                                                }
                                            }
                                            Box(
                                                Modifier
                                                    .padding(4.dp)
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colorScheme.error.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        pendingDeleteSet = set
                                                        showDeleteSetFinalConfirmation = false
                                                    },
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        stringResource(R.string.btn_delete),
                                                        tint = colorScheme.error,
                                                    )
                                                }
                                            }
                                        }
                                        val hintLines = mutableListOf<String>()
                                        if (warmupRecommendation != null) {
                                            val targetWeightText =
                                                referenceWorkingWeight?.let { workingWeight ->
                                                    (workingWeight * warmupRecommendation.percentOfWorkingWeight / 100.0)
                                                        .formatSimple()
                                                }
                                            hintLines.add(
                                                if (targetWeightText != null) {
                                                    stringResource(
                                                        R.string.label_warmup_target_with_weight,
                                                        warmupRecommendation.percentOfWorkingWeight,
                                                        targetWeightText,
                                                    )
                                                } else {
                                                    stringResource(
                                                        R.string.label_warmup_target_percent_only,
                                                        warmupRecommendation.percentOfWorkingWeight,
                                                    )
                                                },
                                            )
                                            warmupRecommendation.reps?.let { reps ->
                                                hintLines.add(
                                                    stringResource(
                                                        R.string.label_warmup_reps_target,
                                                        reps,
                                                    ),
                                                )
                                            }
                                        }
                                        if (!previousSetText.isNullOrBlank()) {
                                            hintLines.add(
                                                stringResource(
                                                    R.string.label_last_time_set,
                                                    previousSetText,
                                                ),
                                            )
                                        }
                                        if (hintLines.isNotEmpty()) {
                                            Text(
                                                text = hintLines.joinToString(" | "),
                                                modifier =
                                                    Modifier.padding(
                                                        start = 12.dp,
                                                        end = 12.dp,
                                                        bottom = 8.dp,
                                                    ),
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurface.copy(alpha = 0.75f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                        onClick = { viewModel.addSet(setGroup) },
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.btn_add_set))
                    }
                }
            }
        }

        item {
            Button(
                modifier =
                    Modifier
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .height(128.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = navToExercisePicker,
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.btn_add_exercise))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(percent = 100),
                    onClick = { showCancelWorkoutDialog = true },
                ) {
                    Text(stringResource(R.string.btn_discard_workout))
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(40.dp),
                    shape = RoundedCornerShape(percent = 100),
                    onClick = { showFinishWorkoutDialog = true },
                ) {
                    Text(stringResource(R.string.btn_finish_workout))
                }
            }
        }
    }
}

private fun setKindLabelRes(setKind: String): Int {
    return when (setKind) {
        SetKinds.WARM_UP -> R.string.set_kind_warm_up
        SetKinds.DROP -> R.string.set_kind_drop
        else -> R.string.set_kind_normal
    }
}

@Composable
private fun ConfirmDeleteSetDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_delete_set)) },
        text = { Text(stringResource(R.string.dialog_message_delete_set_first)) },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(
                    stringResource(R.string.btn_continue),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.btn_cancel),
                )
            }
        },
    )
}

@Composable
private fun ConfirmDeleteSetFinalDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_delete_set_final)) },
        text = { Text(stringResource(R.string.dialog_message_delete_set_final)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    stringResource(R.string.btn_delete),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.btn_cancel),
                )
            }
        },
    )
}

@Composable
private fun CancelWorkoutDialog(
    onDismiss: () -> Unit,
    cancelWorkout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_discard_workout)) },
        confirmButton = {
            Button(onClick = cancelWorkout) {
                Text(
                    stringResource(R.string.btn_delete),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.btn_cancel),
                )
            }
        },
    )
}

@Composable
private fun FinishWorkoutDialog(
    onDismiss: () -> Unit,
    finishWorkout: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_finish_workout)) },
        confirmButton = {
            Button(onClick = finishWorkout) {
                Text(
                    stringResource(R.string.dialog_confirm_finish_workout),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.btn_cancel),
                )
            }
        },
    )
}
