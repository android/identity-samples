/*
 * Copyright 2025 Google LLC
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


package com.google.credentialmanager.sample

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.AppDimensions.bodyFontSize
import com.google.credentialmanager.sample.ui.theme.AppDimensions.circularProgressIndicatorSize
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingExtraSmall
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingSmall
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenPadding
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenTitleFontSize
import com.google.credentialmanager.sample.ui.theme.CredentialManagerSampleTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Stateful composable that displays the sign-up screen of the application.
 * It uses [SignUpViewModel] to handle the sign-up process.
 *
 * @param navController The navigation controller used for screen navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    CredentialManagerSampleTheme {
        val context = LocalContext.current
        val viewModel: SignUpViewModel =
            viewModel(factory = SignUpViewModelFactory(JsonProvider(context)))

        val username by viewModel.username.collectAsState()
        val password by viewModel.password.collectAsState()
        val isPasswordInputVisible by viewModel.isPasswordInputVisible.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val usernameError by viewModel.usernameError.collectAsState()
        val passwordError by viewModel.passwordError.collectAsState()
        val passkeyCreationError by viewModel.passkeyCreationError.collectAsState()
        val passwordCreationError by viewModel.passwordCreationError.collectAsState()

        val activity = context.findActivity()

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collectLatest { event ->
                when (event) {
                    is NavigationEvent.NavigateToHome -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SignUpTitle()
            UsernameInput(username, viewModel::onUsernameChange, usernameError)
            if (isPasswordInputVisible) {
                PasswordInput(password, viewModel::onPasswordChange, passwordError)
            }
            LoadingIndicator(isLoading)
            ErrorMessages(passkeyCreationError, passwordCreationError)
            Spacer(modifier = Modifier.height(0.dp))
            PasskeySignUp(isLoading) {
                activity?.let { activity ->
                    viewModel.signUpWithPasskey {
                        createCredential(activity, it) as CreatePublicKeyCredentialResponse
                    }
                }
            }
            Text(
                text = stringResource(R.string.or_divider),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = paddingSmall)
            )
            PasswordSignUp(isLoading, isPasswordInputVisible) {
                activity?.let { activity ->
                    viewModel.signUpWithPassword { createCredential(activity, it) }
                }
            }
        }
    }
}

/**
 * The title of the sign-up screen.
 */
@Composable
private fun SignUpTitle() {
    Text(
        text = stringResource(R.string.create_new_account),
        fontSize = screenTitleFontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = screenPadding)
    )
}

/**
 * An input field for the user to enter their username.
 *
 * @param username The current username value.
 * @param onUsernameChange The callback to be invoked when the username changes.
 * @param usernameError An error message displayed if the username is blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsernameInput(
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameError: String?
) {
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text(stringResource(R.string.enter_username)) },
        singleLine = true,
        isError = usernameError != null,
        modifier = Modifier.fillMaxWidth()
    )
    if (usernameError != null) {
        Text(usernameError, color = MaterialTheme.colorScheme.error, fontSize = bodyFontSize)
    }
}

/**
 * An input field for the user to enter their password.
 *
 * @param password The current password value.
 * @param onPasswordChange The callback to be invoked when the password changes.
 * @param passwordError An error message displayed if the password is blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.enter_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = passwordError != null,
        modifier = Modifier.fillMaxWidth()
    )
    if (passwordError != null) {
        Text(
            passwordError,
            color = MaterialTheme.colorScheme.error,
            fontSize = bodyFontSize
        )
    }
}

/**
 * A loading indicator to show when an operation is in progress.
 *
 * @param isLoading Whether the loading indicator should be visible.
 */
@Composable
private fun LoadingIndicator(isLoading: Boolean) {
    if (isLoading) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = paddingSmall)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(circularProgressIndicatorSize))
            Spacer(modifier = Modifier.width(paddingSmall))
            Text(stringResource(R.string.operation_in_progress))
        }
    }
}

/**
 * Displays error messages for passkey and password creation.
 *
 * @param passkeyCreationError An error message displayed if passkey creation fails.
 * @param passwordCreationError An error message displayed if password creation fails
 */
@Composable
private fun ErrorMessages(passkeyCreationError: String?, passwordCreationError: String?) {
    if (passkeyCreationError != null) {
        Text(
            passkeyCreationError,
            color = MaterialTheme.colorScheme.error,
            fontSize = bodyFontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = paddingExtraSmall)
        )
    }
    if (passwordCreationError != null) {
        Text(
            passwordCreationError,
            color = MaterialTheme.colorScheme.error,
            fontSize = bodyFontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = paddingExtraSmall)
        )
    }
}

/**
 * A button and descriptive text for signing up with a passkey.
 *
 * @param isLoading Whether an operation is in progress.
 * @param onClick The callback to be invoked when the button is clicked.
 */
@Composable
private fun PasskeySignUp(isLoading: Boolean, onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.passkey_sign_up_description),
        textAlign = TextAlign.Center,
        fontSize = bodyFontSize,
        modifier = Modifier.padding(bottom = paddingSmall)
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(paddingExtraSmall),
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.sign_up_with_passkey))
    }
}

/**
 * A button and descriptive text for signing up with a password.
 *
 * @param isLoading Whether an operation is in progress.
 * @param isPasswordInputVisible Whether the password input is visible.
 * @param onClick The callback to be invoked when the button is clicked.
 */
@Composable
private fun PasswordSignUp(
    isLoading: Boolean,
    isPasswordInputVisible: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.password_sign_up_description),
        textAlign = TextAlign.Center,
        fontSize = bodyFontSize,
        modifier = Modifier.padding(bottom = paddingSmall)
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(paddingExtraSmall),
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (isPasswordInputVisible) stringResource(R.string.sign_up_with_password) else stringResource(
                R.string.sign_up_with_password_instead
            )
        )
    }
}


/**
 * A preview for the [SignUpScreen].
 */
@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    CredentialManagerSampleTheme {
        SignUpScreen(navController = rememberNavController())
    }
}
