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
package com.authentication.shrine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineLoader
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.viewmodel.RegisterUiState
import com.authentication.shrine.ui.viewmodel.RegistrationViewModel

/**
 * Stateful composable function that displays the registration screen.
 *
 * @param navigateToHome Callback for navigating to the home screen.
 * @param viewModel The AuthenticationViewModel that provides the UI state.
 */
@Composable
fun RegisterPasswordScreen(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: RegistrationViewModel,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val uiState = viewModel.uiState.collectAsState().value
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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

    val onRegister = { emailAddress: String, registrationPassword: String ->
        viewModel.onPasswordRegister(
            username = emailAddress,
            password = registrationPassword,
            onSuccess = { flag ->
                createRestoreKey()
                navigateToHome(flag)
            },
            createPassword = { username: String, password: String ->
                credentialManagerUtils.createPassword(
                    username = username,
                    password = password,
                    context = context,
                )
            },
        )
    }

    RegisterPasswordScreen(
        onRegister = onRegister,
        uiState = uiState,
        email = email,
        onEmailChanged = { email = it },
        password = password,
        onPasswordChange = { password = it },
        passwordVisible = passwordVisible,
        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
        onBackClicked = onBackClicked,
        modifier = modifier,
    )
}

/**
 * Stateless composable function that displays the registration screen.
 *
 * This screen allows users to create a new account by providing their email and password.
 * It also supports signing in with passkeys.
 *
 * @param onRegister Callback for logging in with email and password.
 * @param uiState The UI state of the authentication process.
 * @param email The user's email address.
 * @param onEmailChanged Callback for when the email address changes.
 * @param password The user's password.
 * @param onPasswordChange Callback for when the password changes.
 * @param passwordVisible Whether the password is currently visible.
 * @param onPasswordVisibilityToggle Callback for toggling the visibility of the password.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun RegisterPasswordScreen(
    onRegister: (String, String) -> Unit,
    uiState: RegisterUiState,
    email: String,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onBackClicked: () -> Unit,
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
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShrineToolbar(
                showBack = true,
                onBackClicked = onBackClicked,
            )
            ShrineTextHeader(
                text = stringResource(R.string.create_account),
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large)))

            RegisterScreenInputSection(
                email = email,
                onEmailChanged = onEmailChanged,
                password = password,
                passwordVisible = passwordVisible,
                onPasswordChange = onPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onRegister = onRegister,
                isPageLoading = uiState.isLoading,
            )
        }

        if (uiState.isLoading) {
            ShrineLoader()
        }

        val snackbarMessage = stringResource(uiState.messageResourceId)
        if (snackbarMessage.isNotBlank()) {
            LaunchedEffect(uiState) {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage,
                )
            }
        }
    }
}

@Composable
private fun RegisterScreenInputSection(
    email: String,
    onEmailChanged: (String) -> Unit,
    password: String,
    passwordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onRegister: (String, String) -> Unit,
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        TextField(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_standard)),
            value = password,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Password,
                    contentDescription = stringResource(R.string.password_icon),
                )
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            trailingIcon = {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(imageVector = image, stringResource(R.string.password))
                }
            },
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large)))

        ShrineButton(
            onClick = { onRegister(email, password) },
            buttonText = stringResource(R.string.sign_up),
            isButtonEnabled = !isPageLoading,
            modifier = Modifier.widthIn(min = 280.dp),
        )
    }
}

/**
 * Generates a preview of the RegisterPasswordScreen composable function.
 */
@Preview(showSystemUi = true)
@Composable
fun RegisterPasswordScreenPreview() {
    RegisterPasswordScreen(
        onRegister = { _, _ -> },
        uiState = RegisterUiState(),
        email = "",
        onEmailChanged = { },
        password = "",
        onPasswordChange = { },
        passwordVisible = true,
        onPasswordVisibilityToggle = { },
        onBackClicked = { },
    )
}
