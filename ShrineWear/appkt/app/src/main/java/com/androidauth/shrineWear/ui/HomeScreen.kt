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
package com.androidauth.shrineWear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.Graph
import com.androidauth.shrineWear.R

/**
 * Composable function representing the home screen of the application.
 * It handles the authentication flow based on the current authentication status.
 *
 * @param credentialManagerViewModel The {@link CredentialManagerViewModel} to interact with
 * the Credential Manager.
 * @param navigateToLegacyLogin Callback function to navigate to the legacy login screen.
 * @param navigateToSignOut Callback function to navigate to the sign-out screen.
 */
@Composable
fun HomeScreen(
    credentialManagerViewModel: CredentialManagerViewModel,
    navigateToLegacyLogin: () -> Unit,
    navigateToSignOut: () -> Unit,
) {
    val uiState by credentialManagerViewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (!uiState.inProgress) {
        when (Graph.authenticationStatusCode) {
            R.string.credman_status_logged_out -> {
                credentialManagerViewModel.login(context)
            }
            R.string.credman_status_dismissed, R.string.credman_status_no_credentials -> {
                navigateToLegacyLogin()
            }
            R.string.credman_status_authorized -> {
                navigateToSignOut()
            }
            else -> {
                navigateToLegacyLogin()
            }
        }
    } else {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Preview function for the {@link HomeScreen} composable.
 */
@WearPreviewDevices
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        credentialManagerViewModel = CredentialManagerViewModel(),
        navigateToLegacyLogin = {},
        navigateToSignOut = {},
    )
}
