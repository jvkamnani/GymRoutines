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

package com.noahjutz.gymroutines.ui.routines.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Routine
import com.noahjutz.gymroutines.data.domain.RoutineSetGroupWithSets
import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.ui.components.AutoSelectTextField
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.ui.components.durationVisualTransformation
import com.noahjutz.gymroutines.util.RegexPatterns
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun RoutineEditor(
    navToExercisePicker: () -> Unit,
    navToWorkout: (Long) -> Unit,
    popBackStack: () -> Unit,
    routineId: Int,
    exerciseIdsToAdd: List<Int>,
    viewModel: RoutineEditorViewModel = getViewModel { parametersOf(routineId) },
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exerciseIdsToAdd) {
        viewModel.addExercises(exerciseIdsToAdd)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            val isWorkoutRunning by viewModel.isWorkoutInProgress.collectAsState(initial = false)
            if (!isWorkoutRunning) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.startWorkout { id ->
                            navToWorkout(id)
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text(stringResource(R.string.btn_start_workout)) },
                )
            }
        },
        topBar = {
            TopBar(
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                },
                title = stringResource(R.string.screen_edit_routine),
            )
        },
    ) { paddingValues ->
        val routine by viewModel.routine.collectAsState(initial = null)
        Crossfade(routine != null, Modifier.padding(paddingValues)) { isReady ->
            if (!isReady) {
                RoutineEditorPlaceholder()
            } else {
                routine?.let { routine ->
                    RoutineEditorContent(
                        routine = routine.routine,
                        setGroups = routine.setGroups,
                        viewModel = viewModel,
                        navToExercisePicker = navToExercisePicker,
                    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineEditorContent(
    routine: Routine,
    setGroups: List<RoutineSetGroupWithSets>,
    viewModel: RoutineEditorViewModel,
    navToExercisePicker: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 70.dp),
    ) {
        item {
            val (name, setName) = remember { mutableStateOf(routine.name) }
            LaunchedEffect(name) {
                viewModel.updateName(name)
            }
            val (nameLineCount, setNameLineCount) = remember { mutableStateOf(0) }
            BasicTextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 30.dp, end = 30.dp),
                value = name,
                onValueChange = setName,
                onTextLayout = { setNameLineCount(it.lineCount) },
                textStyle = typography.headlineSmall.copy(color = colorScheme.onSurface),
                cursorBrush = SolidColor(colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Surface(
                        modifier = if (nameLineCount <= 1) Modifier.height(60.dp) else Modifier,
                        color = colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(30.dp),
                    ) {
                        Row(
                            Modifier.padding(start = 30.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .padding(vertical = 16.dp)
                                    .weight(1f),
                            ) {
                                if (routine.name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.unnamed_routine),
                                        style =
                                            typography.headlineSmall.copy(
                                                color = colorScheme.onSurface.copy(alpha = 0.12f),
                                            ),
                                    )
                                }
                                innerTextField()
                            }
                            AnimatedVisibility(
                                name.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { setName("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        stringResource(R.string.btn_clear_text),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }

        items(setGroups.sortedBy { it.group.position }, key = { it.group.id }) { setGroup ->
            val exercise = viewModel.getExercise(setGroup.group.exerciseId)!!
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 30.dp),
                shape = RoundedCornerShape(30.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = colorScheme.surface,
                    ),
            ) {
                Column {
                    Surface(Modifier.fillMaxWidth(), color = colorScheme.primary) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                exercise.name,
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
                                        stringResource(R.string.btn_more),
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
                                                setGroups
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
                                                setGroups
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
                    if (setGroup.group.supersetTag != null) {
                        Text(
                            text =
                                stringResource(
                                    R.string.label_superset_tag,
                                    setGroup.group.supersetTag ?: "",
                                ),
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
                            Surface(
                                Modifier
                                    .padding(horizontal = 4.dp, vertical = 8.dp)
                                    .width(110.dp),
                            ) {
                                Text(
                                    stringResource(R.string.column_set_type),
                                    style = headerTextStyle,
                                )
                            }
                            if (exercise.logReps) {
                                Surface(
                                    Modifier
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                        .weight(1f),
                                ) {
                                    Text(
                                        stringResource(R.string.column_reps),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise.logWeight) {
                                Surface(
                                    Modifier
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                        .weight(1f),
                                ) {
                                    Text(
                                        stringResource(R.string.column_weight),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise.logTime) {
                                Surface(
                                    Modifier
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                        .weight(1f),
                                ) {
                                    Text(
                                        stringResource(R.string.column_time),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                            if (exercise.logDistance) {
                                Surface(
                                    Modifier
                                        .padding(horizontal = 4.dp, vertical = 8.dp)
                                        .weight(1f),
                                ) {
                                    Text(
                                        stringResource(R.string.column_distance),
                                        style = headerTextStyle,
                                    )
                                }
                            }
                        }
                        for (set in setGroup.sets) {
                            key(set.routineSetId) {
                                val dismissState = rememberSwipeToDismissBoxState()
                                LaunchedEffect(dismissState.currentValue) {
                                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                        viewModel.deleteSet(set)
                                    }
                                }
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = { SwipeToDeleteBackground(dismissState) },
                                ) {
                                    Surface {
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
                                                            Modifier.padding(
                                                                vertical = 16.dp,
                                                                horizontal = 4.dp,
                                                            ),
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
                                                            .height(56.dp),
                                                    color = colorScheme.onSurface.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                ) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height(56.dp)
                                                                .clickable { typeMenuExpanded = true },
                                                        contentAlignment = Alignment.Center,
                                                    ) {
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
                                            if (exercise.logReps) {
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
                                            if (exercise.logWeight) {
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
                                            if (exercise.logTime) {
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
                                            if (exercise.logDistance) {
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
                        .fillMaxWidth()
                        .padding(30.dp)
                        .height(120.dp),
                shape = RoundedCornerShape(30.dp),
                onClick = navToExercisePicker,
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.btn_add_exercise))
            }
        }
    }
}
