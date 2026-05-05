package com.noahjutz.gymroutines.ui.workout.viewer

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.SetKinds
import com.noahjutz.gymroutines.data.domain.WorkoutWithSetGroups
import com.noahjutz.gymroutines.data.domain.duration
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.pretty
import com.noahjutz.gymroutines.util.toStringOrBlank
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

private fun setKindLabelRes(setKind: String): Int {
    return when (setKind) {
        SetKinds.WARM_UP -> R.string.set_kind_warm_up
        SetKinds.DROP -> R.string.set_kind_drop
        else -> R.string.set_kind_normal
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun WorkoutViewer(
    workoutId: Int,
    viewModel: WorkoutViewerViewModel = getViewModel { parametersOf(workoutId) },
    popBackStack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_view_workout),
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
        val workout by viewModel.workout.collectAsState()
        Crossfade(workout == null, Modifier.padding(paddingValues)) { isNull ->
            if (isNull) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                workout?.let { workout ->
                    WorkoutViewerContent(workout, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalTime
@Composable
fun WorkoutViewerContent(
    workout: WorkoutWithSetGroups,
    viewModel: WorkoutViewerViewModel,
) {
    LazyColumn {
        item {
            val routineName by viewModel.routineName.collectAsState(initial = "")
            Spacer(Modifier.height(24.dp))
            Text(
                text =
                    routineName.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.unnamed_routine),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = typography.headlineSmall,
            )
            Text(
                text = workout.workout.endTime.formatSimple(),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = workout.workout.duration.pretty(),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        items(workout.setGroups.sortedBy { it.group.position }) { setGroup ->
            val exercise by viewModel.getExercise(setGroup.group.exerciseId)
                .collectAsState(initial = null)
            val originalExercise by viewModel.getExercise(setGroup.group.originalExerciseId ?: -1)
                .collectAsState(initial = null)

            Card(
                Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(24.dp),
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
                                        .padding(24.dp)
                                        .weight(1f),
                            )
                        }
                    }
                    if (setGroup.group.originalExerciseId != null || setGroup.group.supersetTag != null) {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
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
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
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
                                Icon(Icons.Default.Check, null)
                            }
                        }
                        for (set in setGroup.sets) {
                            Row(
                                Modifier.padding(horizontal = 4.dp),
                            ) {
                                val TableCell: @Composable RowScope.(
                                    @Composable BoxScope.() -> Unit,
                                ) -> Unit =
                                    {
                                        ProvideTextStyle(
                                            value =
                                                typography.bodyMedium.copy(
                                                    textAlign = TextAlign.Center,
                                                    color = colorScheme.onSurface,
                                                ),
                                        ) {
                                            Surface(
                                                modifier =
                                                    Modifier
                                                        .padding(4.dp)
                                                        .weight(1f),
                                                color =
                                                    colorScheme.onSurface.copy(
                                                        alpha = 0.1f,
                                                    ),
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                Box(
                                                    Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .height(56.dp),
                                                    contentAlignment = Alignment.Center,
                                                    content = it,
                                                )
                                            }
                                        }
                                    }
                                Surface(
                                    modifier =
                                        Modifier
                                            .padding(4.dp)
                                            .width(110.dp),
                                    color = colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(
                                        Modifier.height(56.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(stringResource(setKindLabelRes(set.setKind)))
                                    }
                                }
                                if (exercise?.logReps == true) {
                                    TableCell { Text(set.reps.toStringOrBlank()) }
                                }
                                if (exercise?.logWeight == true) {
                                    TableCell { Text(set.weight.formatSimple()) }
                                }
                                if (exercise?.logTime == true) {
                                    TableCell { Text(set.time.toStringOrBlank()) }
                                }
                                if (exercise?.logDistance == true) {
                                    TableCell { Text(set.distance.formatSimple()) }
                                }
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (set.complete) {
                                                colorScheme.secondary
                                            } else {
                                                colorScheme.onSurface.copy(
                                                    alpha = 0.1f,
                                                )
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (set.complete) {
                                        Icon(
                                            Icons.Default.Check,
                                            stringResource(R.string.column_set_complete),
                                            tint = colorScheme.onSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
