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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ErrorAlertDialog
import com.authentication.shrine.ui.common.PasskeyInfo
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineLoader
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.viewmodel.CreatePasskeyUiState
import com.authentication.shrine.ui.viewmodel.CreatePasskeyViewModel
import com.authentication.shrine.ui.viewmodel.ErrorDialogViewModel

/**
 * Stateful composable function for the create passkey screen.
 *
 * This screen allows the user to create a passkey for authentication.
 *
 * @param navigateToMainMenu Callback to navigate to the main menu.
 * @param viewModel The [CreatePasskeyViewModel] for this screen.
 * @param onLearnMoreClicked Callback to navigate to the learn more screen.
 * @param onNotNowClicked Callback to dismiss the create passkey screen.
 * @param credentialManagerUtils The instance of [CredentialManagerUtils]
 */
@Composable
fun CreatePasskeyScreen(
    navigateToMainMenu: (isSignedInThroughPasskeys: Boolean) -> Unit,
    viewModel: CreatePasskeyViewModel,
    onLearnMoreClicked: () -> Unit,
    onNotNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val uiState = viewModel.uiState.collectAsState().value
    val errorDialogViewModel: ErrorDialogViewModel = hiltViewModel()

    val context = LocalContext.current
    val onRegisterRequest = {
        viewModel.createPasskey(
            onSuccess = { flag ->
                navigateToMainMenu(flag)
            },
        ) { data ->
            credentialManagerUtils.createPasskey(
                requestResult = data,
                context = context,
            )
        }
    }

    CreatePasskeyScreen(
        onLearnMoreClicked = onLearnMoreClicked,
        onRegisterRequest = onRegisterRequest,
        onNotNowClicked = onNotNowClicked,
        showErrorDialog = errorDialogViewModel::showErrorDialog,
        uiState = uiState,
        modifier = modifier,
    )
}

/**
 * Stateless composable function for the create passkey screen.
 *
 * This screen allows the user to create a passkey for authentication.
 *
 * @param onLearnMoreClicked Callback invoked when the user clicks on "Learn more".
 * @param onRegisterRequest Callback to initiate the passkey creation request.
 * @param onNotNowClicked Callback invoked when the user clicks on "Not now".
 * @param showErrorDialog Method that resets the UI state of the Error Dialog
 * @param uiState The current UI state of the create passkey screen.
 * @param modifier Modifier to be applied to the composable.
 */
@Composable
fun CreatePasskeyScreen(
    onLearnMoreClicked: () -> Unit,
    onRegisterRequest: () -> Unit,
    onNotNowClicked: () -> Unit,
    showErrorDialog: () -> Unit,
    uiState: CreatePasskeyUiState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_large))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShrineTextHeader(
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
            showErrorDialog()
            ErrorAlertDialog(
                uiState.errorMessage,
            )
        }
    }
}

@Composable
private fun CreatePasskeyActions(
    onRegisterRequest: () -> Unit,
    uiState: CreatePasskeyUiState,
    onNotNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .wrapContentHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShrineButton(
            onClick = onRegisterRequest,
            buttonText = stringResource(R.string.create_passkey),
            isButtonEnabled = !uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_large)))

        ShrineButton(
            onClick = onNotNowClicked,
            buttonText = stringResource(R.string.not_now),
            isButtonDark = false,
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
            showErrorDialog = { },
            uiState = CreatePasskeyUiState(),
            modifier = Modifier,
        )
    }
}
