package com.noahjutz.gymroutines.ui.settings.data

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.TopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun DataSettings(
    popBackStack: () -> Unit,
    viewModel: DataSettingsViewModel = getViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_data_settings),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.btn_pop_back)
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val exportDatabaseLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportDatabase(uri)
                    viewModel.restartApp()
                }
            }

        val importDatabaseLauncher =
            rememberLauncherForActivityResult(
                object : ActivityResultContracts.OpenDocument() {
                    override fun createIntent(
                        context: Context,
                        input: Array<String>,
                    ): Intent {
                        super.createIntent(context, input)
                        return Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*")
                    }
                },
            ) { uri ->
                if (uri != null) {
                    viewModel.importDatabase(uri)
                    viewModel.restartApp()
                }
            }

        val exportWorkoutCsvLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/csv"),
            ) { uri ->
                if (uri != null) {
                    viewModel.exportWorkoutHistoryCsv(uri)
                }
            }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            val isWorkoutInProgress by viewModel.isWorkoutInProgress.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            val alertFinishWorkout = stringResource(R.string.alert_must_finish_workout)
            val buttonFinishWorkout = stringResource(R.string.btn_finish_workout)
            ListItem(
                modifier =
                    Modifier.clickable {
                        if (isWorkoutInProgress) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    alertFinishWorkout,
                                    actionLabel = buttonFinishWorkout,
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.finishWorkout()
                                    exportDatabaseLauncher.launch(
                                        "gymroutines_${viewModel.getCurrentTimeIso()}.db",
                                    )
                                }
                            }
                        } else {
                            exportDatabaseLauncher.launch(
                                "gymroutines_${viewModel.getCurrentTimeIso()}.db",
                            )
                        }
                    },
                headlineContent = { Text(stringResource(R.string.pref_back_up_data)) },
                supportingContent = { Text(stringResource(R.string.pref_detail_back_up_data)) },
                leadingContent = { Icon(Icons.Default.SaveAlt, null) },
            )
            ListItem(
                modifier =
                    Modifier.clickable {
                        if (isWorkoutInProgress) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(alertFinishWorkout)
                            }
                        } else {
                            importDatabaseLauncher.launch(emptyArray())
                        }
                    },
                headlineContent = { Text(stringResource(R.string.pref_restore_data)) },
                supportingContent = { Text(stringResource(R.string.pref_detail_restore_data)) },
                leadingContent = { Icon(Icons.Default.SettingsBackupRestore, null) },
            )
            ListItem(
                modifier =
                    Modifier.clickable {
                        exportWorkoutCsvLauncher.launch(
                            "gymroutines_workouts_${viewModel.getCurrentTimeIso()}.csv",
                        )
                    },
                headlineContent = { Text(stringResource(R.string.pref_export_workout_csv)) },
                supportingContent = { Text(stringResource(R.string.pref_detail_export_workout_csv)) },
                leadingContent = { Icon(Icons.Default.FileDownload, null) },
            )
        }
    }
}
