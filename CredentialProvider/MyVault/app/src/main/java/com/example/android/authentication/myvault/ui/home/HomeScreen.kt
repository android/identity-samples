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
package com.example.android.authentication.myvault.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.data.PasswordItem
import com.example.android.authentication.myvault.data.room.SiteWithCredentials

/**
 * This composable holds the stateful version of Home screen
 * @param homeViewModel : viewmodel instance handling business logic for Home Screen
 * @param openDrawer : method to open the drawer on click
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    HomeScreen(
        openDrawer,
        uiState,
        homeViewModel::onPasskeyDelete,
        homeViewModel::onPasswordDelete,
        hasShownCredentials,
        currentSiteId,
        modifier,
    )
}

/**
 * This class holds the stateless version of Home screen to ease preview
 * @param openDrawer : method to open the drawer on click
 * @param uiState : MutableStateFlow to retrieve updated state from viewmodel
 * @param onPasskeyDelete : Method to be called on passkey delete button click
 * @param onPasswordDelete : Method to be called on password delete button click
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun HomeScreen(
    openDrawer: () -> Unit,
    uiState: HomeUiState,
    onPasskeyDelete: (PasskeyItem) -> Unit,
    onPasswordDelete: (PasswordItem) -> Unit,
    hasShownCredentials: MutableState<Boolean>,
    currentSiteId: MutableLongState,
    modifier: Modifier = Modifier,
) {
    val site = uiState.siteList.find { it.site.id == currentSiteId.longValue }
    if (site != null && hasShownCredentials.value) {
        ShowCredentialsScreen(
            modifier = modifier,
            site = site,
            onCancel = { hasShownCredentials.value = false },
            onPasswordDelete = {
                onPasswordDelete(it)
            },
            onPasskeyDelete = {
                onPasskeyDelete(it)
            },
        )
    } else {
        HomeScreenContent(
            openDrawer = openDrawer,
            sites = uiState.siteList,
            iconMap = uiState.iconMap,
            { siteId ->
                currentSiteId.longValue = siteId
                hasShownCredentials.value = true
            },
            modifier,
        )
    }
}

/**
 * This composable contain the UI logic rendered on Home screen
 *
 * @param openDrawer The method to open the drawer on click.
 * @param sites The list of sites with credentials.
 * @param iconMap The map of site names to their corresponding icons.
 * @param onSiteSelected The callback to be invoked when a site is selected
 * @param modifier The modifier to be applied to the composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    openDrawer: () -> Unit,
    sites: List<SiteWithCredentials>,
    iconMap: Map<String, Bitmap>,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBarContent(openDrawer)
        },
        modifier = modifier,
    ) { innerPadding ->
        CredentialsList(
            sites = sites,
            iconMap = iconMap,
            onSiteSelected = onSiteSelected,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarContent(openDrawer: () -> Unit) {
    CenterAlignedTopAppBar(
        modifier = Modifier,
        title = {
            Text(
                text = stringResource(R.string.credentials)
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
 * This composable function provides a preview of the HomeScreen composable.
 */
@Preview
@Composable
fun HomeScreenPreview() {
    val hasShownCredentials = rememberSaveable { mutableStateOf(false) }
    val currentSiteId = rememberSaveable { mutableLongStateOf(0L) }

    HomeScreen(
        openDrawer = {},
        uiState = HomeUiState(),
        onPasswordDelete = {},
        onPasskeyDelete = {},
        hasShownCredentials = hasShownCredentials,
        currentSiteId = currentSiteId,
        modifier = Modifier,
    )
}
