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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.common.ShrineButton
import com.example.android.authentication.shrine.ui.common.ShrineLoader
import com.example.android.authentication.shrine.ui.common.TextHeader
import com.example.android.authentication.shrine.ui.viewmodel.RegisterUiState
import com.example.android.authentication.shrine.ui.viewmodel.RegistrationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Stateful composable function that displays the registration screen.
 *
 * @param navigateToHome Callback for navigating to the home screen.
 * @param viewModel The AuthenticationViewModel that provides the UI state.
 */
@Composable
fun RegisterScreen(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: RegistrationViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState.collectAsState().value
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val onRegister = { email: String, password: String ->
        viewModel.onRegister(email, password) { flag ->
            navigateToHome(flag)
        }
    }

    RegisterScreen(
        onRegister = onRegister,
        uiState = uiState,
        email = email,
        onEmailChanged = { email = it },
        password = password,
        onPasswordChange = { password = it },
        passwordVisible = passwordVisible,
        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
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
fun RegisterScreen(
    onRegister: (String, String) -> Unit,
    uiState: RegisterUiState,
    email: String,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { contentPadding ->
        Column(
            modifier = modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.dimen_20))
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextHeader(
                text = stringResource(R.string.create_account),
            )

            Spacer(Modifier.padding(dimensionResource(R.dimen.dimen_20)))

            RegisterScreenInputSection(
                email = email,
                onEmailChanged = onEmailChanged,
                password = password,
                passwordVisible = passwordVisible,
                onPasswordChange = onPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
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
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onRegister: (String, String) -> Unit,
    isPageLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
            )
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.dimen_16)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_16)),
            value = email,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = stringResource(R.string.emailicon),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            onValueChange = onEmailChanged,
            label = { Text(stringResource(R.string.e_mail_address)) },
            placeholder = { Text(stringResource(R.string.e_mail_address)) },
        )
        TextField(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_16)),
            value = password,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Password,
                    contentDescription = stringResource(R.string.passwordicon),
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
        Spacer(Modifier.padding(dimensionResource(R.dimen.dimen_20)))

        val waitSigningYouIn = stringResource(R.string.wait_signing_you_in)
        val enterValidUserNameAndPassword =
            stringResource(R.string.enter_valid_username_and_password)
        ShrineButton(
            onClick = {
                onSignUpClicked(
                    email,
                    password,
                    coroutineScope,
                    snackbarHostState,
                    waitSigningYouIn,
                    onRegister,
                    enterValidUserNameAndPassword,
                )
            },
            buttonText = stringResource(R.string.sign_up),
            isButtonEnabled = !isPageLoading,
            modifier = Modifier.widthIn(min = dimensionResource(R.dimen.dimen_280)),
        )
    }
}

private fun onSignUpClicked(
    email: String,
    password: String,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    waitSigningYouIn: String,
    onRegister: (String, String) -> Unit,
    enterValidUserNameAndPassword: String,
) {
    if (email.isNotEmpty() && password.isNotEmpty()) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = waitSigningYouIn,
            )
        }
        onRegister(email, password)
    } else {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = enterValidUserNameAndPassword,
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
    RegisterScreen(
        onRegister = { _, _ -> },
        uiState = RegisterUiState(),
        email = "",
        onEmailChanged = { },
        password = "",
        onPasswordChange = { },
        passwordVisible = true,
        onPasswordVisibilityToggle = { },
    )
}
