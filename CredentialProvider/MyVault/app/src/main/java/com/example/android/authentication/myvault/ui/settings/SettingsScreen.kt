/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.authentication.myvault.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.authentication.myvault.R

/**
 * This composable holds the stateful version of Settings screen
 * @param viewModel : viewmodel instance handling business logic for Settings Screen
 * @param openDrawer : method to open the drawer on click
 * @param modifier : Modifier to update behavior of composables UI
 */
@Composable
fun SettingsScreen(
    openDrawer: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    SettingsScreen(
        viewModel::deleteAllData,
        openDrawer,
        uiState,
        snackbarHostState,
        modifier,
    )
}

/**
 * This composable holds the stateless version of Home screen to ease preview
 * @param openDrawer : method to open the drawer on click
 * @param onDeleteClicked : Method to be called on "Delete all credentials" click
 * @param uiState : MutableStateFlow to retrieve updated state from viewmodel
 * @param snackbarHostState : State of the SnackbarHost, which controls the queue and the current Snackbar being shown inside
 * @param modifier : Modifier to update behavior of composables UI
 */
@Composable
fun SettingsScreen(
    onDeleteClicked: () -> Unit,
    openDrawer: () -> Unit,
    uiState: SettingsViewModel.UiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppBarContent(openDrawer, Modifier)
        },
        modifier = modifier,
    ) { innerPadding ->
        DeleteCredentialsButton(onDeleteClicked, innerPadding, Modifier)
    }
    when (uiState) {
        is SettingsViewModel.UiState.Init -> {
            Log.w(stringResource(R.string.myvault), stringResource(R.string.initialized))
        }

        is SettingsViewModel.UiState.Success -> {
            LaunchedEffect(uiState) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.data_deleted),
                    null,
                    false,
                    SnackbarDuration.Short,
                )
            }
        }
    }
}

/**
 * Set the top AppBar UI
 *
 * @param openDrawer : method to open the drawer on click
 * @param modifier  Modifier to update behavior of composables UI
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppBarContent(openDrawer: () -> Unit, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.settings),
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.credentials),
                )
            }
        },
    )
}

/**
 * Set the Delete Button UI & action
 *
 * @param onDeleteClicked Method to be called on "Delete all credentials" click
 * @param innerPadding   PaddingValues to apply to the button
 * @param modifier   Modifier to update behavior of composables UI
 */
@Composable
private fun DeleteCredentialsButton(
    onDeleteClicked: () -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopCenter)
            .padding(innerPadding),
        onClick = {
            onDeleteClicked()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = stringResource(R.string.delete_all_data))
    }
}

/**
 * This composable function provides a preview of the SettingsScreen composable.
 */
@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        onDeleteClicked = { },
        openDrawer = {},
        uiState = SettingsViewModel.UiState.Init,
        snackbarHostState = SnackbarHostState(),
        modifier = Modifier,
    )
}
