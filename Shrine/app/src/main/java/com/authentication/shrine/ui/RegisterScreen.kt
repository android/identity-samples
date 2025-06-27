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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineClickableText
import com.authentication.shrine.ui.common.ShrineLoader
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.grayBackground
import com.authentication.shrine.ui.viewmodel.RegisterUiState
import com.authentication.shrine.ui.viewmodel.RegistrationViewModel

/**
 * Stateful composable function that displays the registration screen.
 *
 * @param navigateToHome Callback for navigating to the home screen.
 * @param viewModel The AuthenticationViewModel that provides the UI state.
 */
@Composable
fun RegisterScreen(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onOtherWaysToSignInClicked: () -> Unit,
    onBackClicked: () -> Unit,
    viewModel: RegistrationViewModel,
    modifier: Modifier = Modifier,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val uiState = viewModel.uiState.collectAsState().value
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    val context = LocalContext.current
    val createRestoreKey = {
        viewModel.createRestoreKey(
            createRestoreKeyOnCredMan = { createRestoreCredObject ->
                credentialManagerUtils.createRestoreKey(
                    context = context,
                    requestResult = createRestoreCredObject,
                )
            },
        )
    }

    val onPasskeyRegister = { emailAddress: String ->
        viewModel.onPasskeyRegister(
            username = emailAddress,
            onSuccess = { flag ->
                createRestoreKey()
                navigateToHome(flag)
            },
            createPasskeyCallback = { data ->
                credentialManagerUtils.createPasskey(
                    requestResult = data,
                    context = context,
                )
            },
        )
    }

    RegisterScreen(
        onPasskeyRegister = onPasskeyRegister,
        uiState = uiState,
        fullName = fullName,
        onFullNameChanged = { fullName = it },
        onLearnMoreClicked = onLearnMoreClicked,
        onOtherWaysToSignInClicked = onOtherWaysToSignInClicked,
        onBackClicked = onBackClicked,
        email = email,
        onEmailChanged = { email = it },
        modifier = modifier,
    )
}

/**
 * Stateless composable function that displays the registration screen.
 *
 * This screen allows users to create a new account by creating a passkey.
 *
 * @param onPasskeyRegister Callback for logging in with passkey.
 * @param uiState The UI state of the authentication process.
 * @param fullName The user's name.
 * @param email The user's email address.
 * @param onEmailChanged Callback for when the email address changes.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun RegisterScreen(
    onPasskeyRegister: (String) -> Unit,
    uiState: RegisterUiState,
    fullName: String,
    onFullNameChanged: (String) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onOtherWaysToSignInClicked: () -> Unit,
    onBackClicked: () -> Unit,
    email: String,
    onEmailChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(dimensionResource(R.dimen.padding_small))
                .fillMaxSize()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShrineToolbar(
                showBack = true,
                onBackClicked = onBackClicked,
            )
            ShrineTextHeader(
                text = stringResource(R.string.sign_up),
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large)))

            RegisterScreenInputSection(
                fullName = fullName,
                onFullNameChanged = onFullNameChanged,
                onLearnMoreClicked = onLearnMoreClicked,
                onOtherWaysToSignInClicked = onOtherWaysToSignInClicked,
                email = email,
                onEmailChanged = onEmailChanged,
                onPasskeyRegister = onPasskeyRegister,
                isPageLoading = uiState.isLoading,
            )
        }

        if (uiState.isLoading) {
            ShrineLoader()
        }

        val snackbarMessage = stringResource(uiState.messageResourceId)
        if (snackbarMessage.isNotBlank()) {
            LaunchedEffect(snackbarMessage) {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage,
                )
            }
        }

        val snackbarErrorMessage = uiState.errorMessage
        if (!snackbarErrorMessage.isNullOrBlank()) {
            LaunchedEffect(snackbarErrorMessage) {
                snackbarHostState.showSnackbar(
                    message = snackbarErrorMessage,
                )
            }
        }
    }
}

@Composable
private fun RegisterScreenInputSection(
    fullName: String,
    onFullNameChanged: (String) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onOtherWaysToSignInClicked: () -> Unit,
    email: String,
    onEmailChanged: (String) -> Unit,
    onPasskeyRegister: (String) -> Unit,
    isPageLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
            )
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.dimen_standard)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
    ) {
        Text(
            text = stringResource(R.string.full_name),
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
        )
        TextField(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
            value = fullName,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.email_icon),
                )
            },
            singleLine = true,
            onValueChange = onFullNameChanged,
            label = { Text(stringResource(R.string.full_name)) },
            placeholder = { Text(stringResource(R.string.full_name)) },
        )
        Text(
            text = stringResource(R.string.username),
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
        )
        TextField(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
            value = email,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = stringResource(R.string.email_icon),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            onValueChange = onEmailChanged,
            label = { Text(stringResource(R.string.email_address)) },
            placeholder = { Text(stringResource(R.string.email_address)) },
        )
        Text(
            text = stringResource(R.string.signing_in),
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
        )

        PasskeyInformationTab(onLearnMoreClicked, onOtherWaysToSignInClicked)

        ShrineButton(
            onClick = { onPasskeyRegister(email) },
            buttonText = stringResource(R.string.sign_up),
            isButtonEnabled = !isPageLoading,
            modifier = Modifier.widthIn(min = 280.dp),
        )
    }
}

/**
 * Composable for the Passkeys Information Tab UI Element
 *
 * @param onLearnMoreClicked lambda for more information, navigates to an informational screen
 * @param onOtherWaysToSignInClicked lambda for other sign in methods, navigates to
 * @OtherOptionsSignInScreen
 * */
@Composable
private fun PasskeyInformationTab(
    onLearnMoreClicked: () -> Unit,
    onOtherWaysToSignInClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.size_standard)))
            .background(grayBackground)
            .padding(dimensionResource(R.dimen.padding_large)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        ) {
            Column(
                modifier = Modifier.weight(0.6F),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            ) {
                ShrineClickableText(
                    text = stringResource(R.string.signing_in_description),
                    clickableText = stringResource(R.string.how_passkeys_work),
                    onTextClick = onLearnMoreClicked,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                ShrineClickableText(
                    text = "",
                    clickableText = stringResource(R.string.other_ways_to_sign_in),
                    onTextClick = onOtherWaysToSignInClicked,
                    textStyle = TextStyle(color = Color(0xFF006B5F)),
                )
            }

            Image(
                modifier = Modifier.weight(0.2F),
                painter = painterResource(R.drawable.ic_passkeys_info),
                contentDescription = stringResource(R.string.passkey_icon),
            )
        }
    }
}

/**
 * Generates a preview of the RegisterScreen composable function.
 */
@Preview(showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    ShrineTheme {
        RegisterScreen(
            onPasskeyRegister = { _ -> },
            onLearnMoreClicked = { },
            onOtherWaysToSignInClicked = { },
            onBackClicked = { },
            uiState = RegisterUiState(),
            fullName = "",
            onFullNameChanged = { },
            email = "",
            onEmailChanged = { },
        )
    }
}
