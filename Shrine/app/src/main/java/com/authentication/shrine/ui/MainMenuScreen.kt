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
package com.authentication.shrine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.LogoHeading
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.greenBackground
import com.authentication.shrine.ui.viewmodel.HomeViewModel

/**
 * Stateful composable function that displays the main menu screen.
 *
 * @param onShrineButtonClicked Callback for when the Shrine button is clicked.
 * @param onSettingsButtonClicked Callback for when the settings button is clicked.
 * @param onHelpButtonClicked Callback for when the help button is clicked.
 * @param navigateToLogin Callback for navigating to the login screen.
 * @param viewModel The HomeViewModel that provides the UI state.
 */
@Composable
fun MainMenuScreen(
    onShrineButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onHelpButtonClicked: () -> Unit,
    navigateToLogin: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val onSignOut = {
        viewModel.signOut(
            deleteRestoreKey = credentialManagerUtils::deleteRestoreKey,
        )
    }

    MainMenuScreen(
        onShrineButtonClicked = onShrineButtonClicked,
        onSettingsButtonClicked = onSettingsButtonClicked,
        onHelpButtonClicked = onHelpButtonClicked,
        navigateToLogin = navigateToLogin,
        onSignOut = onSignOut,
        modifier = modifier,
    )
}

/**
 * Stateless composable function for the main menu screen.
 *
 * This screen provides options for accessing the Shrine app, settings, help, and signing out.
 *
 * @param modifier Modifier to be applied to the composable.
 * @param onShrineButtonClicked Callback to navigate to the Shrine app.
 * @param onSettingsButtonClicked Callback to navigate to the settings screen.
 * @param onHelpButtonClicked Callback to navigate to the help screen.
 * @param navigateToLogin Callback to navigate to the login screen.
 * @param onSignOut Callback to sign out the user.
 */
@Composable
fun MainMenuScreen(
    onShrineButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onHelpButtonClicked: () -> Unit,
    navigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .background(greenBackground)
                .padding(contentPadding)
                .fillMaxHeight()
                .padding(dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LogoHeading()

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.size_large)))

            MainMenuButtonsList(
                onShrineButtonClicked,
                onSettingsButtonClicked,
                onHelpButtonClicked,
                onSignOut,
                navigateToLogin,
            )
        }
    }
}

/**
 * Composable function that displays a list of buttons for the main menu.
 *
 * @param onShrineButtonClicked Callback invoked when the "Shop" button is clicked.
 * @param onSettingsButtonClicked Callback invoked when the "Settings" button is clicked.
 * @param onHelpButtonClicked Callback invoked when the "Help" button is clicked.
 * @param onSignOut Callback invoked when the "Sign Out" button is clicked.
 * @param navigateToLogin Callback invoked to navigate to the login screen after signing out.
 */
@Composable
private fun MainMenuButtonsList(
    onShrineButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onHelpButtonClicked: () -> Unit,
    onSignOut: () -> Unit,
    navigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_large)),
    ) {
        ShrineButton(
            onClick = onShrineButtonClicked,
            buttonText = stringResource(R.string.shop),
        )

        ShrineButton(
            onClick = onSettingsButtonClicked,
            buttonText = stringResource(R.string.settings),
            isButtonDark = false,
        )

        ShrineButton(
            onClick = onHelpButtonClicked,
            buttonText = stringResource(R.string.help),
        )

        ShrineButton(
            onClick = {
                onSignOut()
                navigateToLogin()
            },
            buttonText = stringResource(R.string.sign_out),
            isButtonDark = false,
        )
    }
}

/**
 * Generates a preview of the PasskeysSignedPreview composable function.
 */
@Preview(showBackground = true)
@Composable
fun PasskeysSignedPreview() {
    ShrineTheme {
        MainMenuScreen(
            onShrineButtonClicked = { },
            onSettingsButtonClicked = { },
            onHelpButtonClicked = { },
            navigateToLogin = { },
            onSignOut = { },
        )
    }
}
