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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.android.authentication.shrine.CredentialManagerUtils
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.common.ErrorAlertDialog
import com.example.android.authentication.shrine.ui.common.LogoHeading
import com.example.android.authentication.shrine.ui.common.ShrineButton
import com.example.android.authentication.shrine.ui.common.ShrineLoader
import com.example.android.authentication.shrine.ui.theme.ShrineTheme
import com.example.android.authentication.shrine.ui.theme.light_button
import com.example.android.authentication.shrine.ui.viewmodel.AuthenticationUiState
import com.example.android.authentication.shrine.ui.viewmodel.AuthenticationViewModel
import com.example.android.authentication.shrine.ui.viewmodel.ErrorDialogViewModel

/**
 * Stateful composable function for Authentication screen.
 *
 * This screen allows the user to authenticate using a username and password, or through passkeys.
 *
 * @param navigateToHome Callback to navigate to the home screen.
 * @param viewModel The [AuthenticationViewModel] for this screen.
 * @param navigateToRegister Callback to navigate to the registration screen.
 * @param credentialManagerUtils The instance of [CredentialManagerUtils]
 */
@Composable
fun AuthenticationScreen(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: AuthenticationViewModel,
    navigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val uiState = viewModel.uiState.collectAsState().value
    val errorDialogViewModel: ErrorDialogViewModel = hiltViewModel()

    // Passing in the lambda / context to the VM
    val context = LocalContext.current
    val onSignInWithPasskeysRequest = {
        viewModel.signInWithPasskeysRequest(
            onSuccess = { flag ->
                navigateToHome(flag)
            },
            getPasskey = { jsonObject ->
                credentialManagerUtils.getPasskey(
                    context = context,
                    creationResult = jsonObject,
                )
            },
            createRestoreCredential = { createRestoreCredObject ->
                credentialManagerUtils.createRestoreKey(
                    context = context,
                    requestResult = createRestoreCredObject,
                )
            },
        )
    }

    viewModel.checkForStoredRestoreKey(
        getRestoreKey = { requestResult ->
            credentialManagerUtils.getRestoreKey(requestResult, context)
        },
    )

    AuthenticationScreen(
        onSignInWithPasskeysRequest = onSignInWithPasskeysRequest,
        navigateToRegister = navigateToRegister,
        showErrorDialog = errorDialogViewModel::showErrorDialog,
        uiState = uiState,
        modifier = modifier,
    )
}

/**
 * Stateless composable function for the authentication screen.
 *
 * This screen allows the user to authenticate using a username and password, or through passkeys.
 *
 * @param modifier Modifier to be applied to the composable.
 * @param onSignInWithPasskeysRequest Callback to initiate the sign-in with passkeys request.
 * @param showErrorDialog Method that resets the UI state of the Error Dialog
 * @param navigateToRegister Callback to navigate to the registration screen.
 * @param uiState The current UI state of the authentication screen.
*/
@Composable
fun AuthenticationScreen(
    onSignInWithPasskeysRequest: () -> Unit,
    navigateToRegister: () -> Unit,
    showErrorDialog: () -> Unit,
    uiState: AuthenticationUiState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_extra_large)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoHeading()

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large)))

            // Sign In Button
            ShrineButton(
                onClick = onSignInWithPasskeysRequest,
                buttonText = stringResource(id = R.string.sign_in),
                isButtonEnabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_large)))

            // Sign Up Button
            ShrineButton(
                onClick = navigateToRegister,
                buttonText = stringResource(id = R.string.sign_up),
                backgroundColor = light_button,
                isButtonEnabled = !uiState.isLoading,
            )
        }

        if (uiState.isLoading) {
            ShrineLoader()
        }

        if (!uiState.passkeyRequestErrorMessage.isNullOrBlank()) {
            showErrorDialog()
            ErrorAlertDialog(uiState.passkeyRequestErrorMessage)
        }

        if (!uiState.isSignInWithPasskeysSuccess) {
            val snackbarMessage = stringResource(uiState.passkeyResponseMessageResourceId)
            if (snackbarMessage.isNotBlank()) {
                LaunchedEffect(uiState) {
                    snackbarHostState.showSnackbar(
                        message = snackbarMessage,
                    )
                }
            }
        }
    }
}

/**
 * Preview function for the authentication screen.
 *
 * This function displays a preview of the authentication screen in the Android Studio preview pane.
 */
@Preview(showSystemUi = true)
@Composable
fun AuthenticationScreenPreview() {
    ShrineTheme {
        AuthenticationScreen(
            onSignInWithPasskeysRequest = { },
            navigateToRegister = { },
            showErrorDialog = { },
            uiState = AuthenticationUiState(),
            modifier = Modifier,
        )
    }
}
