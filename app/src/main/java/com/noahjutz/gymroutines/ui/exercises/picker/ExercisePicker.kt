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

package com.noahjutz.gymroutines.ui.exercises.picker

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@ExperimentalAnimationApi
@Composable
fun ExercisePickerSheet(
    targetSetGroupId: Int = -1,
    viewModel: ExercisePickerViewModel = getViewModel { parametersOf(targetSetGroupId) },
    singleSelect: Boolean = false,
    onExercisesSelected: (List<Int>) -> Unit,
    navToExerciseEditor: () -> Unit,
) {
    val allExercises by viewModel.allExercises.collectAsState(emptyList())
    val selectedExerciseIds by viewModel.selectedExerciseIds.collectAsState(initial = emptyList())
    Scaffold { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            TopBar(
                title = stringResource(R.string.screen_pick_exercise),
                navigationIcon = {
                    IconButton(
                        onClick = { onExercisesSelected(emptyList()) },
                    ) { Icon(Icons.Default.Close, stringResource(R.string.btn_cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = { onExercisesSelected(selectedExerciseIds) },
                        enabled = selectedExerciseIds.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.btn_select_option))
                    }
                },
            )
            val searchQuery by viewModel.nameFilter.collectAsState()
            SearchBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                value = searchQuery,
                onValueChange = viewModel::search,
            )
            LazyColumn(Modifier.weight(1f)) {
                items(allExercises.filter { !it.hidden }) { exercise ->
                    val checked by viewModel.exercisesContains(exercise)
                        .collectAsState(initial = false)
                    ListItem(
                        modifier =
                            Modifier.toggleable(
                                value = checked,
                                onValueChange = {
                                    if (it) {
                                        if (singleSelect) {
                                            viewModel.setSingleExercise(exercise)
                                        } else {
                                            viewModel.addExercise(exercise)
                                        }
                                    } else {
                                        viewModel.removeExercise(exercise)
                                    }
                                },
                            ),
                        leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                        headlineContent = {
                            Text(exercise.name)
                        },
                    )
                }

                item {
                    ListItem(
                        modifier = Modifier.clickable(onClick = navToExerciseEditor),
                        leadingContent = {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.btn_new_exercise),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
            }
        }
    }
}
