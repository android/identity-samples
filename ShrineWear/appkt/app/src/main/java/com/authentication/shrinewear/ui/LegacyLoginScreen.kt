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
package com.authentication.shrinewear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.authentication.shrinewear.Graph
import com.authentication.shrinewear.R
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

private const val TAG = "LegacyLoginScreen"
private const val WARNING_LEGACY_SIWG_UNAVAILABLE =
    "Legacy Google Sign in not available on devices running SDK 35+ with Credential Manager. Use Sign in with Google via Credential Manager to sign in."

/**
 * Composable for the header of the legacy login options list.
 */
@Composable
fun LegacyLoginHeader() {
    ListHeader {
        Text(
            stringResource(R.string.legacy_login_options),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Composable for the "Sign in with OAuth" button.
 *
 * @param navigateToOAuth Callback to be invoked when the button is clicked.
 */
@Composable
fun OAuthLoginButton(navigateToOAuth: () -> Unit) {
    Button(
        onClick = { navigateToOAuth() },
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = stringResource(R.string.sign_in_with_oauth),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
    )
}

/**
 * Composable for the "Sign in with Google" button for legacy authentication.
 * This button is disabled on devices running SDK 35+ with Credential Manager enabled.
 *
 * @param navigateToLegacySignInWithGoogle Callback to be invoked when the button is clicked
 * (only on devices where Credential Manager is not enabled).
 * @param isCredentialManagerEnabled Flag indicating whether the Credential Manager is enabled.
 */
@Composable
fun LegacySignInWithGoogleButton(
    navigateToLegacySignInWithGoogle: () -> Unit,
    isCredentialManagerEnabled: Boolean,
) {
    Button(
        onClick = {
            if (!isCredentialManagerEnabled) {
                navigateToLegacySignInWithGoogle()
            }
        },
        enabled = !isCredentialManagerEnabled,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = stringResource(R.string.sign_in_with_google),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        secondaryLabel = {
            Text(
                text = stringResource(R.string.disabled_on_sdk_34_label),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        icon = { Icon(Icons.Filled.AccountCircle, stringResource(R.string.siwg_icon)) },
    )
}

/**
 * Composable for the "Cancel" button on the legacy login screen.
 * Clicking this button navigates the user back to the home screen and resets the
 * authentication status to logged out.
 *
 * @param navigateToHome Callback to be invoked when the button is clicked.
 */
@Composable
fun CancelLoginButton(navigateToHome: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            Graph.authenticationStatusCode = R.string.credman_status_logged_out
            navigateToHome()
        },
        label = {
            Text(
                text = stringResource(R.string.cancel_button_label),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        secondaryLabel = {
            Text(
                text = stringResource(R.string.back_to_home_screen_label),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
    )
}

/**
 * Composable for the legacy login screen, providing options for OAuth and legacy Google Sign-in.
 *
 * @param navigateToOAuth Callback to navigate to the OAuth login flow.
 * @param navigateToLegacySignInWithGoogle Callback to navigate to the legacy Google Sign-in flow.
 * @param navigateToHome Callback to navigate to the home screen.
 * @param isCredentialManagerEnabled Flag indicating whether the Credential Manager is enabled.
 */
@Composable
fun LegacyLoginScreen(
    navigateToOAuth: () -> Unit,
    navigateToLegacySignInWithGoogle: () -> Unit,
    navigateToHome: () -> Unit,
    isCredentialManagerEnabled: Boolean,
) {
    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold {
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = columnState,
            contentPadding = rememberResponsiveColumnPadding(),
        ) {
            item { LegacyLoginHeader() }
            item { OAuthLoginButton(navigateToOAuth = navigateToOAuth) }
            item {
                LegacySignInWithGoogleButton(
                    navigateToLegacySignInWithGoogle = navigateToLegacySignInWithGoogle,
                    isCredentialManagerEnabled = isCredentialManagerEnabled,
                )
            }
            item { CancelLoginButton(navigateToHome = navigateToHome) }
        }
    }
}

/**
 * Preview for the [LegacyLoginHeader] composable.
 */
@WearPreviewDevices
@Composable
fun LegacyLoginHeaderPreview() {
    LegacyLoginHeader()
}

/**
 * Preview for the [OAuthLoginButton] composable.
 */
@WearPreviewDevices
@Composable
fun OAuthLoginButtonPreview() {
    OAuthLoginButton(navigateToOAuth = {})
}

/**
 * Preview for the [LegacySignInWithGoogleButton] composable.
 */
@WearPreviewDevices
@Composable
fun LegacySignInWithGoogleButtonPreview() {
    LegacySignInWithGoogleButton(
        navigateToLegacySignInWithGoogle = {},
        isCredentialManagerEnabled = true,
    )
}

/**
 * Preview for the [CancelLoginButton] composable.
 */
@WearPreviewDevices
@Composable
fun CancelLoginButtonPreview() {
    CancelLoginButton(navigateToHome = {})
}

/**
 * Preview for the [LegacyLoginScreen] composable.
 */
@WearPreviewDevices
@Composable
fun LegacyLoginScreenPreview() {
    LegacyLoginScreen(
        navigateToOAuth = {},
        navigateToLegacySignInWithGoogle = {},
        navigateToHome = {},
        isCredentialManagerEnabled = true,
    )
}
