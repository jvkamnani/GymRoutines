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

package com.noahjutz.gymroutines.ui.routines.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.SwipeToDeleteBackground
import com.noahjutz.gymroutines.ui.components.TopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@ExperimentalAnimationApi
@Composable
fun RoutineList(
    navToRoutineEditor: (Long) -> Unit,
    navToSettings: () -> Unit,
    viewModel: RoutineListViewModel = getViewModel(),
) {
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_routine_list),
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.addRoutine(
                        onComplete = { id ->
                            navToRoutineEditor(id)
                        },
                    )
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.btn_new_routine)) },
            )
        },
    ) { paddingValues ->
        val routines by viewModel.routines.collectAsState(null)

        Crossfade(routines != null, Modifier.padding(paddingValues)) { isReady ->
            if (isReady) {
                RoutineListContent(
                    routines = routines ?: emptyList(),
                    navToRoutineEditor = navToRoutineEditor,
                    viewModel = viewModel,
                )
            } else {
                RoutineListPlaceholder()
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RoutineListContent(
    routines: List<RoutineListViewModel.RoutineListEntry>,
    navToRoutineEditor: (Long) -> Unit,
    viewModel: RoutineListViewModel,
) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxHeight()) {
        item {
            val nameFilter by viewModel.nameFilter.collectAsState()
            SearchBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                value = nameFilter,
                onValueChange = viewModel::setNameFilter,
            )
        }

        items(items = routines, key = { "${it.routine.routineId}:${it.displayName}" }) { entry ->
            val routine = entry.routine
            val dismissState = rememberSwipeToDismissBoxState()

            SwipeToDismissBox(
                modifier =
                    Modifier
                        .animateItemPlacement(),
                // .zIndex(if (dismissState.offset.value == 0f) 0f else 1f),
                state = dismissState,
                backgroundContent = { SwipeToDeleteBackground(dismissState) },
            ) {
                Card(
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 0.dp,
                            draggedElevation = 4.dp,
                        ),
                ) {
                    ListItem(
                        modifier = Modifier.clickable { navToRoutineEditor(routine.routineId.toLong()) },
                        headlineContent = {
                            Text(
                                text = entry.displayName.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.unnamed_routine),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(Icons.Default.MoreVert, null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    DropdownMenuItem(
                                        onClick = {
                                            expanded = false
                                            scope.launch {
                                                dismissState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                                            }
                                        },
                                        text = {
                                            Text(stringResource(R.string.btn_delete))
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }

            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                AlertDialog(title = {
                    Text(
                        stringResource(
                            R.string.dialog_title_delete,
                            routine.name.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.unnamed_routine),
                        ),
                    )
                }, confirmButton = {
                    Button(
                        onClick = { viewModel.deleteRoutine(routine.routineId) },
                        content = { Text(stringResource(R.string.btn_delete)) },
                    )
                }, dismissButton = {
                    TextButton(
                        onClick = { scope.launch { dismissState.reset() } },
                        content = { Text(stringResource(R.string.btn_cancel)) },
                    )
                }, onDismissRequest = { scope.launch { dismissState.reset() } })
            }
        }
        item {
            // Fix FAB overlap
            Box(Modifier.height(72.dp)) {}
        }
    }
}
