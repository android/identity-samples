/*
 * Copyright 2025 The Android Open Source Project
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
package com.authentication.shrinewear.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.authentication.shrinewear.AuthenticationState
import com.authentication.shrinewear.Graph
import com.authentication.shrinewear.R
import com.authentication.shrinewear.ui.viewmodel.CredentialManagerViewModel

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
    val inProgress by credentialManagerViewModel.inProgress.collectAsState()
    val authenticationState by Graph.authenticationState.collectAsState()
    val activity = LocalActivity.current as ComponentActivity

    DemoInstructions()

    if (!inProgress) {
        when (authenticationState) {
            AuthenticationState.LOGGED_IN -> {
                navigateToSignOut()
            }

            AuthenticationState.LOGGED_OUT -> {
                credentialManagerViewModel.login(activity)
            }

            AuthenticationState.DISMISSED_BY_USER, AuthenticationState.MISSING_CREDENTIALS -> {
                navigateToLegacyLogin()
            }

            else -> {
                navigateToLegacyLogin()
            }
        }
    } else {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    }
}


object DemoInstructionsState {
    var isFirstLaunch: Boolean = true
}

/**
 * Displays an [AlertDialog] containing introductory demo instructions for the user.
 *
 * This dialog is shown upon the initial launch of the application and can be dismissed
 * by the user.
 *
 * Note: The `AlertDialog` API used here (`edgeButton` and `visible`) might be from an
 * older or specific alpha version of `androidx.wear.compose.material3`. For newer
 * versions, consider using `confirmButton` and `dismissButton` for actions, and
 * conditionally rendering the dialog using an `if` statement.
 */
@Composable
private fun DemoInstructions() {
    if (!DemoInstructionsState.isFirstLaunch) {
        return
    }

    AlertDialog(
        visible = true,
        onDismissRequest = {
            DemoInstructionsState.isFirstLaunch = false
        },
        edgeButton = {
            AlertDialogDefaults.EdgeButton(onClick = {
                DemoInstructionsState.isFirstLaunch = false
            })
        },
        title = {
            Text(
                text = stringResource(R.string.shrine_sample),
                textAlign = TextAlign.Center,
            )
        },
        text = { Text(stringResource(R.string.see_readme_md_for_usage_directions)) },
    )
}

/**
 * Preview for the [DemoInstructions] composable.
 *
 * This preview renders the dialog with the demo instructions as it would appear on Wear OS devices.
 */
@WearPreviewDevices
@Composable
fun DemoInstructionsPreview() {
    DemoInstructions()
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
