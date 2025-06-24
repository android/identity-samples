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
package com.authentication.shrinewear.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.authentication.shrinewear.R
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

/**
 * Composable that displays the header for the OAuth login screen.
 */
@Composable
fun OAuthLoginHeader() {
    ListHeader {
        Text(
            stringResource(R.string.oauth_login_screen),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Composable for the "Sign In with OAuth" button.
 *
 * @param oAuthViewModel The [OAuthViewModel] to trigger the OAuth sign-in process.
 */
@Composable
fun SignInWithOAuthButton(oAuthViewModel: OAuthViewModel) {
    Button(
        onClick = { oAuthViewModel.signInWithOauth() },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = stringResource(R.string.authorize_device),
                textAlign = TextAlign.Center,
            )
        },
    )
}

/**
 * Composable for the "Cancel" button on the OAuth screen, which navigates back to legacy login options.
 *
 * @param navigateToLegacyLogin A lambda function to navigate to the legacy login options screen.
 */
@Composable
fun CancelOAuthButton(navigateToLegacyLogin: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { navigateToLegacyLogin() },
        label = { Text(text = stringResource(R.string.cancel_button_label)) },
        secondaryLabel = {
            Text(text = stringResource(R.string.back_to_legacy_options_button_label))
        },
    )
}

/**
 * The main composable for the OAuth login screen.
 *
 * This screen displays options for OAuth sign-in, shows the status of the authorization process,
 * and provides navigation options.
 *
 * @param oAuthViewModel The [OAuthViewModel] managing the OAuth sign-in state and actions.
 * @param navigateToSignOut A lambda function to navigate to the sign-out screen upon successful authorization.
 * @param navigateToLegacyLogin A lambda function to navigate back to legacy login options if the user cancels.
 */
@Composable
fun OAuthScreen(
    oAuthViewModel: OAuthViewModel,
    navigateToSignOut: () -> Unit,
    navigateToLegacyLogin: () -> Unit,
) {
    val uiState by oAuthViewModel.uiState.collectAsState()
    val columnState = rememberTransformingLazyColumnState()

    LaunchedEffect(key1 = uiState.statusCode) {
        if (uiState.statusCode == R.string.status_authorized) {
            navigateToSignOut()
        }
    }

    ScreenScaffold {
        TransformingLazyColumn(
            state = columnState,
            contentPadding = rememberResponsiveColumnPadding(),
        ) {
            item { OAuthLoginHeader() }
            item { SignInWithOAuthButton(oAuthViewModel = oAuthViewModel) }
            item { Text(stringResource(id = uiState.statusCode)) }
            item { Text(uiState.resultMessage) }
            item { CancelOAuthButton(navigateToLegacyLogin = navigateToLegacyLogin) }
        }
    }
}

/**
 * Preview for the [OAuthLoginHeader] composable.
 */
@WearPreviewDevices
@Composable
private fun PreviewOAuthLoginHeader() {
    OAuthLoginHeader()
}

/**
 * Preview for the [CancelOAuthButton] composable.
 */
@WearPreviewDevices
@Composable
private fun PreviewCancelOAuthButton() {
    CancelOAuthButton(navigateToLegacyLogin = {})
}

/**
 * Preview for the [OAuthScreen] composable.
 *
 * This preview provides dummy implementations for the ViewModel and navigation actions.
 */
@WearPreviewDevices
@Composable
fun OAuthScreenPreview() {
    OAuthScreen(
        oAuthViewModel = OAuthViewModel(application = Application()),
        navigateToSignOut = {},
        navigateToLegacyLogin = {},
    )
}
