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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.SearchBar
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
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
@Composable
fun RoutineListContent(
    routines: List<RoutineListViewModel.RoutineListEntry>,
    navToRoutineEditor: (Long) -> Unit,
    viewModel: RoutineListViewModel,
) {
    var firstDeleteConfirmEntry by remember { mutableStateOf<RoutineListViewModel.RoutineListEntry?>(null) }
    var finalDeleteConfirmEntry by remember { mutableStateOf<RoutineListViewModel.RoutineListEntry?>(null) }

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
            val completedTextColor = Color(0xFF2E7D32)
            val completedCardColor = Color(0xFFE8F5E9)
            Card(
                modifier = Modifier.animateItemPlacement(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (entry.isCompleted) completedCardColor else MaterialTheme.colorScheme.surface,
                    ),
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
                            color = if (entry.isCompleted) completedTextColor else Color.Unspecified,
                        )
                    },
                    supportingContent = {
                        if (entry.isCompleted) {
                            Text(
                                text = stringResource(R.string.label_routine_completed),
                                color = completedTextColor,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.label_routine_pending),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
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
                                        firstDeleteConfirmEntry = entry
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
        item {
            // Fix FAB overlap
            Box(Modifier.height(72.dp)) {}
        }
    }

    firstDeleteConfirmEntry?.let { entry ->
        AlertDialog(
            title = {
                Text(
                    stringResource(
                        R.string.dialog_title_delete,
                        entry.routine.name.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.unnamed_routine),
                    ),
                )
            },
            text = { Text(stringResource(R.string.dialog_message_delete_routine_first)) },
            confirmButton = {
                Button(
                    onClick = {
                        firstDeleteConfirmEntry = null
                        finalDeleteConfirmEntry = entry
                    },
                    content = { Text(stringResource(R.string.btn_continue)) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { firstDeleteConfirmEntry = null },
                    content = { Text(stringResource(R.string.btn_cancel)) },
                )
            },
            onDismissRequest = { firstDeleteConfirmEntry = null },
        )
    }

    finalDeleteConfirmEntry?.let { entry ->
        AlertDialog(
            title = { Text(stringResource(R.string.dialog_title_delete_routine_final)) },
            text = { Text(stringResource(R.string.dialog_message_delete_routine_final)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRoutine(entry.routine.routineId)
                        finalDeleteConfirmEntry = null
                    },
                    content = { Text(stringResource(R.string.btn_delete)) },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { finalDeleteConfirmEntry = null },
                    content = { Text(stringResource(R.string.btn_cancel)) },
                )
            },
            onDismissRequest = { finalDeleteConfirmEntry = null },
        )
    }
}
