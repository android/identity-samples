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
package com.example.android.authentication.shrine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.common.ErrorAlertDialog
import com.example.android.authentication.shrine.ui.common.PasskeyInfo
import com.example.android.authentication.shrine.ui.common.ShrineButton
import com.example.android.authentication.shrine.ui.common.ShrineLoader
import com.example.android.authentication.shrine.ui.common.TextHeader
import com.example.android.authentication.shrine.ui.theme.ShrineTheme
import com.example.android.authentication.shrine.ui.theme.light_button
import com.example.android.authentication.shrine.ui.viewmodel.CreatePasskeyUiState
import com.example.android.authentication.shrine.ui.viewmodel.CreatePasskeyViewModel

/**
 * Stateful composable function for the create passkey screen.
 *
 * This screen allows the user to create a passkey for authentication.
 *
 * @param navigateToMainMenu Callback to navigate to the main menu.
 * @param viewModel The [CreatePasskeyViewModel] for this screen.
 * @param onLearnMoreClicked Callback to navigate to the learn more screen.
 * @param onNotNowClicked Callback to dismiss the create passkey screen.
 */
@Composable
fun CreatePasskeyScreen(
    navigateToMainMenu: (isSignedInThroughPasskeys: Boolean) -> Unit,
    viewModel: CreatePasskeyViewModel,
    onLearnMoreClicked: () -> Unit,
    onNotNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState.collectAsState().value

    val onRegisterRequest = {
        viewModel.createPasskey { flag ->
            navigateToMainMenu(flag)
        }
    }

    CreatePasskeyScreen(
        onLearnMoreClicked = onLearnMoreClicked,
        onRegisterRequest = onRegisterRequest,
        onNotNowClicked = onNotNowClicked,
        uiState = uiState,
        modifier = modifier,
    )
}

/**
 * Stateless composable function for the create passkey screen.
 *
 * This screen allows the user to create a passkey for authentication.
 *
 * @param modifier Modifier to be applied to the composable.
 * @param onLearnMoreClicked Callback invoked when the user clicks on "Learn more".
 * @param onRegisterRequest Callback to initiate the passkey creation request.
 * @param onNotNowClicked Callback invoked when the user clicks on "Not now".
 * @param uiState The current UI state of the create passkey screen.
 */
@Composable
fun CreatePasskeyScreen(
    onLearnMoreClicked: () -> Unit,
    onRegisterRequest: () -> Unit,
    onNotNowClicked: () -> Unit,
    uiState: CreatePasskeyUiState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.dimen_24))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextHeader(
                text = stringResource(R.string.create_passkey),
            )
            PasskeyInfo(
                onLearnMoreClicked = onLearnMoreClicked,
            )
            CreatePasskeyActions(
                onRegisterRequest = onRegisterRequest,
                uiState = uiState,
                onNotNowClicked = onNotNowClicked,
            )
        }

        if (uiState.isLoading) {
            ShrineLoader()
        }

        val messageId = uiState.messageResourceId
        if (messageId != null) {
            val snackbarMessage = stringResource(messageId)
            LaunchedEffect(uiState) {
                snackbarHostState.showSnackbar(snackbarMessage)
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            ErrorAlertDialog(uiState.errorMessage)
        }
    }
}

@Composable
private fun CreatePasskeyActions(
    onRegisterRequest: () -> Unit,
    uiState: CreatePasskeyUiState,
    onNotNowClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.dimen_250)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShrineButton(
            onClick = onRegisterRequest,
            buttonText = stringResource(R.string.create_passkey),
            isButtonEnabled = !uiState.isLoading,
        )

        Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.dimen_12)))

        ShrineButton(
            onClick = onNotNowClicked,
            buttonText = stringResource(R.string.not_now),
            backgroundColor = light_button,
            isButtonEnabled = !uiState.isLoading,
        )
    }
}

/**
 * Preview function for the create passkey screen.
 *
 * This function displays a preview of the create passkey screen in the Android Studio preview pane.
 */
@Preview(showSystemUi = true)
@Composable
fun CreatePasskeyScreenPreview() {
    ShrineTheme {
        CreatePasskeyScreen(
            onLearnMoreClicked = { },
            onRegisterRequest = { },
            onNotNowClicked = { },
            uiState = CreatePasskeyUiState(),
            modifier = Modifier,
        )
    }
}
