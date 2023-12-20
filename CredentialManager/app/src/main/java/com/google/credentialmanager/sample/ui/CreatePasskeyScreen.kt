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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.PasswordCredential
import com.google.credentialmanager.sample.Graph
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.PasskeyInfo
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.common.TextHeader
import com.google.credentialmanager.sample.ui.theme.light_button
import com.google.credentialmanager.sample.ui.viewmodel.AuthUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun CreatePasskeyRoute(
    navigateToMainMenu: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: HomeViewModel,
    onLearnMoreClicked: () -> Unit,
    onNotNowClicked: () -> Unit,
    scope: CoroutineScope
) {

    val uiState = viewModel.uiState.collectAsState().value
    CreatePasskeyScreen(
        navigateToMainMenu,
        onLearnMoreClicked,
        viewModel::registerRequest,
        viewModel::registerResponse,
        onNotNowClicked,
        uiState,
        scope
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CreatePasskeyScreen(
    navigateToMainMenu: (flag: Boolean) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onRegisterRequest: () -> Unit,
    onRegisterResponse: (CreatePublicKeyCredentialResponse) -> Unit,
    onNotNowClicked: () -> Unit,
    uiState: HomeUiState,
    scope: CoroutineScope
) {
    val coroutineScope = rememberCoroutineScope()
    val auth = Graph.auth
    val activity = LocalContext.current as Activity

    var enabled1 by remember { mutableStateOf(false) }
    var enabled2 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextHeader(text = stringResource(R.string.create_passkey))
        PasskeyInfo(onLearnMoreClicked)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShrineButton(
                onClick = {
                    //Create a passkey for your account
                    onRegisterRequest()
                },
                enabled = enabled1
            ) {
                Text(text = stringResource(R.string.create_passkey))
            }

            Spacer(modifier = Modifier.padding(10.dp))

            ShrineButton(
                onClick = { onNotNowClicked ()},
                color = light_button,
                ) {
                Text(text = stringResource(R.string.not_now))
            }
        }

        //Handle UiState values
        when (uiState) {
            is HomeUiState.Empty -> {
                enabled1 = true
                enabled2 = true
            }

            is HomeUiState.IsLoading -> {
                enabled1 = false
                enabled2 = false
            }

            is HomeUiState.CreationResult ->
                LaunchedEffect(uiState) {
                    val data = auth.createPasskey(activity, uiState.data)
                    if (data != null) {
                        onRegisterResponse(data)
                        navigateToMainMenu(true)
                    } else {
                        navigateToMainMenu(false)
                    }
                    enabled1 = true
                    enabled2 = true
                }

            is HomeUiState.MsgString -> {
                LaunchedEffect(uiState) {
                    Toast.makeText(activity, uiState.msg, Toast.LENGTH_LONG).show()
                }
                enabled1 = true
                enabled2 = true
            }
        }
    }

    BackHandler(enabled = true) {
        activity.finish()
    }
}

