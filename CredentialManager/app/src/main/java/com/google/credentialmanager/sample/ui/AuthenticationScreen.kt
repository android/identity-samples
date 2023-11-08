/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.credentialmanager.sample.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import com.google.credentialmanager.sample.Graph
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.LogoHeading
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.theme.light_button
import com.google.credentialmanager.sample.ui.viewmodel.AuthUiState
import com.google.credentialmanager.sample.ui.viewmodel.AuthenticationViewModel

@Composable
fun AuthenticationRoute(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: AuthenticationViewModel,
    navigateToRegister: () -> Unit,
) {

    val uiState = viewModel.uiState.collectAsState().value

    AuthenticationScreen(
        navigateToHome,
        viewModel::signInRequest,
        viewModel::signInResponse,
        navigateToRegister,
        uiState
    )
}

// This is the main landing page with Sign-In and Sign Up Buttons
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun AuthenticationScreen(
    navigateToHome: (flag: Boolean) -> Unit,
    onSignInRequest: () -> Unit,
    onSignInResponse: (GetCredentialResponse) -> Unit,
    navigateToRegister: () -> Unit,
    uiState: AuthUiState
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var enabled1 by remember { mutableStateOf(false) }
    var enabled2 by remember { mutableStateOf(false) }

    val auth = Graph.auth
    val activity = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoHeading()
        Spacer(modifier = Modifier.padding(20.dp))
        ShrineButton(
            onClick = {
                //Fetch credentials for your account
                      onSignInRequest()
                      },
            enabled = enabled2
        ) {
            Text(text = stringResource(id = R.string.sign_in))
        }

        Spacer(modifier = Modifier.padding(10.dp))

        ShrineButton(
            color = light_button,
            onClick = navigateToRegister
        ) {
            Text(text = stringResource(id = R.string.sign_up))
        }

    }

    //Handle UiState values
    when (uiState) {
        is AuthUiState.Empty -> {
            enabled1 = true
            enabled2 = true
        }

        is AuthUiState.IsLoading -> {
            enabled1 = false
            enabled2 = false
        }

        is AuthUiState.RequestResult -> LaunchedEffect(uiState) {
            enabled1 = true
            enabled2 = true
            val data = auth.getPasskey(activity, uiState.data)
            data?.let {
                Toast.makeText(
                    activity, "Wait for server validation. Letting you in", Toast.LENGTH_LONG
                ).show()
                onSignInResponse(data)
            }
        }

        is AuthUiState.MsgString -> {
            if (uiState.success && uiState.request == "signin") {
                if (uiState.credential != null && uiState.credential is PasswordCredential) {
                    navigateToHome(true)
                } else {
                    navigateToHome(false)
                }
            } else {
                LaunchedEffect(uiState) {
                    Toast.makeText(activity, uiState.msg, Toast.LENGTH_LONG).show()
                }
            }
            enabled1 = true
            enabled2 = true
        }

        is AuthUiState.LoginResult -> {
            if (uiState.flag) {
                LaunchedEffect(uiState) {
                    auth.createPassword(email, password, activity)
                }
                navigateToHome(true)
            } else {
                LaunchedEffect(uiState) {
                    Toast.makeText(activity, uiState.msg, Toast.LENGTH_LONG).show()
                }
            }
            enabled1 = true
            enabled2 = true
        }
        else -> {}
    }
}