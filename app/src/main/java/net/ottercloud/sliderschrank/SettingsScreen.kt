/*
 * Copyright (c) 2025 OtterCloud
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.ottercloud.sliderschrank

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import java.util.Locale
import kotlinx.coroutines.launch
import net.ottercloud.sliderschrank.data.AppDatabase
import net.ottercloud.sliderschrank.util.DummyDataGenerator
import net.ottercloud.sliderschrank.util.SettingsManager

private enum class ResetDialogState {
    None,
    First,
    Second
}

private enum class DummyDataDialogState {
    None,
    Confirm
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val successMessage = stringResource(R.string.dummy_data_added_success)

    val backgroundOptions = AppBackground.entries
    val currentBackground by settingsManager.background.collectAsState(
        initial = AppBackground.CORK
    )
    val isDummyDataAdded by settingsManager.isDummyDataAdded.collectAsState(
        initial = false
    )

    var resetDialogState by rememberSaveable { mutableStateOf(ResetDialogState.None) }
    var dummyDataDialogState by rememberSaveable { mutableStateOf(DummyDataDialogState.None) }
    var expanded by remember { mutableStateOf(false) }

    val languageOptions = AppLanguage.entries
    val currentLocale = if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
        AppCompatDelegate.getApplicationLocales()[0]
    } else {
        null
    }
    val currentLanguage = if (currentLocale != null) {
        AppLanguage.fromCode(currentLocale.language)
    } else {
        AppLanguage.fromCode(Locale.getDefault().language)
    }
    var languageExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.language_setting_title))
                ExposedDropdownMenuBox(expanded = languageExpanded, onExpandedChange = {
                    languageExpanded = !languageExpanded
                }) {
                    TextField(
                        value = stringResource(currentLanguage.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                        },
                        modifier = Modifier.menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        )
                    )
                    ExposedDropdownMenu(expanded = languageExpanded, onDismissRequest = {
                        languageExpanded =
                            false
                    }) {
                        languageOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(stringResource(selectionOption.labelRes)) },
                                onClick = {
                                    val appLocale: LocaleListCompat =
                                        LocaleListCompat.forLanguageTags(
                                            selectionOption.code
                                        )
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                    languageExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.background_setting_title))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
                    expanded = !expanded
                }) {
                    TextField(
                        value = stringResource(currentBackground.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        backgroundOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(stringResource(selectionOption.labelRes)) },
                                onClick = {
                                    scope.launch {
                                        settingsManager.setBackground(selectionOption)
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { resetDialogState = ResetDialogState.First }) {
                    Text(stringResource(R.string.reset_app_title))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        dummyDataDialogState = DummyDataDialogState.Confirm
                    },
                    enabled = !isDummyDataAdded
                ) {
                    Text(stringResource(R.string.add_dummy_data))
                }
            }
        }
    }

    when (dummyDataDialogState) {
        DummyDataDialogState.Confirm -> {
            AlertDialog(
                onDismissRequest = { dummyDataDialogState = DummyDataDialogState.None },
                title = { Text(stringResource(R.string.add_dummy_data)) },
                text = { Text(stringResource(R.string.add_dummy_data_confirmation)) },
                confirmButton = {
                    TextButton(onClick = {
                        dummyDataDialogState = DummyDataDialogState.None
                        scope.launch {
                            DummyDataGenerator.generateDummyData(context, database)
                            settingsManager.setDummyDataAdded(true)
                            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dummyDataDialogState = DummyDataDialogState.None }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        DummyDataDialogState.None -> { /* No dialog */ }
    }

    when (resetDialogState) {
        ResetDialogState.First -> {
            AlertDialog(
                onDismissRequest = { resetDialogState = ResetDialogState.None },
                title = { Text(stringResource(R.string.reset_app_title)) },
                text = {
                    Text(stringResource(R.string.reset_data_confirmation))
                },
                confirmButton = {
                    TextButton(onClick = {
                        resetDialogState = ResetDialogState.Second
                    }) {
                        Text(stringResource(R.string.reset))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { resetDialogState = ResetDialogState.None }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        ResetDialogState.Second -> {
            AlertDialog(
                onDismissRequest = { resetDialogState = ResetDialogState.None },
                title = { Text(stringResource(R.string.last_warning)) },
                text = {
                    Text(
                        stringResource(R.string.reset_data_confirmation_second)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            settingsManager.clearData()
                        }
                        resetDialogState = ResetDialogState.None
                    }) {
                        Text(stringResource(R.string.delete_everything))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { resetDialogState = ResetDialogState.None }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        ResetDialogState.None -> { /* No dialog */ }
    }
}