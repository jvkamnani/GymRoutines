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

package com.noahjutz.gymroutines.ui.workout.insights

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.material3.placeholder
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Workout
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.ui.components.SimpleLineChart
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel
import kotlin.time.ExperimentalTime

@ExperimentalFoundationApi
@ExperimentalTime
@Composable
fun WorkoutInsights(
    viewModel: WorkoutInsightsViewModel = getViewModel(),
    navToSettings: () -> Unit,
    navToWorkoutEditor: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_insights),
                actions = {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.btn_more))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                onClick = navToSettings,
                                text = { Text(stringResource(R.string.screen_settings)) },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        val workouts by viewModel.workouts.collectAsState(initial = null)
        val routineNames by viewModel.routineNames.collectAsState(initial = null)
        val exerciseProgressCharts by viewModel.exerciseProgressCharts.collectAsState(initial = null)
        var pendingDeleteWorkout by remember { mutableStateOf<Workout?>(null) }
        var showDeleteWorkoutFinalConfirmation by remember { mutableStateOf(false) }

        val pendingDeleteWorkoutName =
            pendingDeleteWorkout?.workoutId?.let { workoutId ->
                routineNames?.get(workoutId)?.takeIf { it.isNotBlank() }
            } ?: stringResource(R.string.unnamed_routine)

        if (pendingDeleteWorkout != null && !showDeleteWorkoutFinalConfirmation) {
            DeleteConfirmation(
                name = pendingDeleteWorkoutName,
                onConfirm = { showDeleteWorkoutFinalConfirmation = true },
                onDismiss = { pendingDeleteWorkout = null },
                confirmLabel = stringResource(R.string.btn_continue),
                message = stringResource(R.string.dialog_message_delete_workout_first),
            )
        }

        if (pendingDeleteWorkout != null && showDeleteWorkoutFinalConfirmation) {
            DeleteConfirmation(
                name = pendingDeleteWorkoutName,
                onConfirm = {
                    pendingDeleteWorkout?.let(viewModel::delete)
                    pendingDeleteWorkout = null
                    showDeleteWorkoutFinalConfirmation = false
                },
                onDismiss = {
                    pendingDeleteWorkout = null
                    showDeleteWorkoutFinalConfirmation = false
                },
                confirmLabel = stringResource(R.string.btn_delete),
                message = stringResource(R.string.dialog_message_delete_workout_final),
            )
        }

        LazyColumn(contentPadding = paddingValues) {
            item {
                Box(Modifier.padding(16.dp)) {
                    WorkoutCharts(workouts)
                }
            }

            item {
                Text(
                    stringResource(R.string.chart_section_exercise_progress),
                    Modifier.padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                    style = typography.headlineSmall,
                )
            }

            if (exerciseProgressCharts == null) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .height(200.dp)
                                .placeholder(visible = true),
                    )
                }
            } else if (exerciseProgressCharts?.isEmpty() == true) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.chart_no_exercise_data),
                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                items(exerciseProgressCharts ?: emptyList(), { it.exerciseId }) { chart ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ExerciseProgressChartCard(chart)
                    }
                }
            }

            item { Spacer(Modifier.size(8.dp)) }

            stickyHeader {
                Surface(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.screen_workout_history),
                        Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        style = typography.headlineSmall,
                    )
                }
            }

            if (workouts != null && routineNames != null) {
                items(workouts ?: emptyList(), { it.workoutId }) { workout ->
                    val routineName =
                        routineNames?.get(workout.workoutId)?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.unnamed_routine)

                    Card(
                        onClick = { navToWorkoutEditor(workout.workoutId) },
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp,
                                draggedElevation = 4.dp,
                            ),
                    ) {
                        ListItem(headlineContent = {
                            Text(
                                text = routineName,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }, trailingContent = {
                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    DropdownMenuItem(onClick = {
                                        expanded = false
                                        pendingDeleteWorkout = workout
                                        showDeleteWorkoutFinalConfirmation = false
                                    }, text = {
                                        Text(stringResource(R.string.btn_delete))
                                    })
                                }
                            }
                        })
                    }
                }
            } else {
                items(5) {
                    ListItem(headlineContent = {
                        Text(
                            "A".repeat((5..15).random()),
                            Modifier.placeholder(visible = true),
                        )
                    })
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmation(
    name: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                stringResource(
                    R.string.dialog_title_delete,
                    name,
                ),
            )
        },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                content = { Text(confirmLabel) },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = { Text(stringResource(R.string.btn_cancel)) },
            )
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ExerciseProgressChartCard(chart: ExerciseProgressChart) {
    val metricName =
        when (chart.metric) {
            ProgressMetric.WEIGHT -> stringResource(R.string.column_weight)
            ProgressMetric.REPS -> stringResource(R.string.column_reps)
            ProgressMetric.TIME -> stringResource(R.string.column_time)
            ProgressMetric.DISTANCE -> stringResource(R.string.column_distance)
        }
    ChartCard(
        title = "${chart.exerciseName} • $metricName",
        subtitle = stringResource(R.string.label_latest_value, chart.latestValueLabel),
    ) {
        if (chart.points.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.chart_insufficient_data),
                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        } else {
            val smoothedData =
                chart.points.chunked(3).map { chunk ->
                    val avg = chunk.map { it.second }.average()
                    chunk.first().first to avg.toFloat()
                }
            SimpleLineChart(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                data = smoothedData,
                secondaryData = chart.points,
            )
        }
    }
}

@ExperimentalTime
@Composable
private fun WorkoutCharts(workouts: List<Workout>?) {
    ChartCard(title = stringResource(R.string.chart_workout_duration), subtitle = null) {
        when {
            workouts == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .placeholder(visible = true),
                )
            }

            workouts.size < 3 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.chart_insufficient_data),
                        color = colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            else -> {
                SimpleLineChart(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    data =
                        workouts.reversed().mapIndexed { i, workout ->
                            Pair(i.toFloat(), workout.duration.inWholeSeconds.toFloat())
                        }.chunked(3) {
                            val avg = it.map { it.second }.average()
                            Pair(it.first().first, avg.toFloat())
                        },
                    secondaryData =
                        workouts.reversed().mapIndexed { i, workout ->
                            Pair(i.toFloat(), workout.duration.inWholeSeconds.toFloat())
                        },
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String?,
    chart: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(
                Modifier.fillMaxWidth(),
                color = colorScheme.primary,
            ) {
                Column {
                    Text(
                        title,
                        Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
                        style = typography.headlineSmall,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            Modifier.padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
                            style = typography.bodyMedium,
                        )
                    }
                }
            }
            chart()
        }
    }
}
