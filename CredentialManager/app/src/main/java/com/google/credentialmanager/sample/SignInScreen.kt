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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.theme.AppDimensions.bodyFontSize
import com.google.credentialmanager.sample.ui.theme.AppDimensions.circularProgressIndicatorSize
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingExtraSmall
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingMedium
import com.google.credentialmanager.sample.ui.theme.AppDimensions.paddingSmall
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenPadding
import com.google.credentialmanager.sample.ui.theme.AppDimensions.screenTitleFontSize
import com.google.credentialmanager.sample.ui.theme.CredentialManagerSampleTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Stateful composable that displays the sign-in screen of the application.
 *
 * It uses[SignInViewModel] to handle the sign-in process.
 *
 * @param navController The navigation controller used for screen navigation.
 */
@Composable
fun SignInScreen(navController: NavController) {
    CredentialManagerSampleTheme {
        val context = LocalContext.current
        val viewModel: SignInViewModel =
            viewModel(factory = SignInViewModelFactory(JsonProvider(context)))

        val isLoading by viewModel.isLoading.collectAsState()
        val signInError by viewModel.signInError.collectAsState()

        val activity = context.findActivity()

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collectLatest { event ->
                when (event) {
                    is NavigationEvent.NavigateToHome -> {
                        DataProvider.setSignedInThroughPasskeys(event.signedInWithPasskeys)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignIn.route) { inclusive = true }
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
            verticalArrangement = Arrangement.spacedBy(paddingMedium)
        ) {
            SignInTitle()
            LoadingIndicator(isLoading)
            ErrorMessage(signInError)
            SignInButton(isLoading) {
                activity?.let { activity ->
                    viewModel.signIn {
                        getCredential(activity, it)
                    }
                }
            }
        }
    }
}

/**
 * The title of the sign-in screen.
 */
@Composable
private fun SignInTitle() {
    Text(
        text = stringResource(R.string.sign_in),
        fontSize = screenTitleFontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = screenPadding, bottom = paddingMedium)
    )
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
 * Displays an error message if the sign-in process fails.
 *
 * @param signInError An optional error message to display.
 */
@Composable
private fun ErrorMessage(signInError: String?) {
    signInError?.let {
        Text(
            stringResource(R.string.error_while_authenticating, it),
            color = MaterialTheme.colorScheme.error,
            fontSize = bodyFontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = paddingExtraSmall)
        )
    }
}

/**
 * A button that initiates the sign-in process.
 *
 * @param isLoading Whether an operation is in progress.
 * @param onClick The callback to be invoked when the button is clicked.
 */
@Composable
private fun SignInButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(paddingExtraSmall),
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.sign_in_with_passkey_saved_password))
    }
}

/**
 * A preview for the [SignInScreen].
 */
@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    CredentialManagerSampleTheme {
        SignInScreen(navController = rememberNavController())
    }
}
