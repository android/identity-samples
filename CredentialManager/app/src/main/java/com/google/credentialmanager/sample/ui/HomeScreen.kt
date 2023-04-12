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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CreatePublicKeyCredentialResponse
import com.google.credentialmanager.sample.Graph
import com.google.credentialmanager.sample.ui.viewmodel.AuthUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    navigateToLogin: () -> Unit,
    viewModel: HomeViewModel
) {

    val uiState = viewModel.uiState.collectAsState().value
    HomeScreen(
        navigateToLogin,
        viewModel::registerRequest,
        viewModel::registerResponse,
        viewModel::signOut,
        uiState
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun HomeScreen(
    navigateToLogin: () -> Unit,
    onRegisterRequest: () -> Unit,
    onRegisterResponse: (CreatePublicKeyCredentialResponse) -> Unit,
    onSignOut: () -> Unit,
    uiState: HomeUiState
) {

    val coroutineScope = rememberCoroutineScope()
    val auth = Graph.auth
    val activity = LocalContext.current as Activity

    var enabled1 by remember { mutableStateOf(false) }
    var enabled2 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(20.dp),
            text = "Save a passkey",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center,
            text = "Sign in to your account easily and securely with a passkey. " +
                "Note: Your biometric data is only stored on your devices and will never be shared with anyone",
            fontSize = 20.sp
        )

        Row(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(
                onClick = {
                    //Create a passkey for your account
                    onRegisterRequest()
                },
                enabled = enabled1
            ) {
                Text("Save a passkey")
            }
        }

        Text(
            modifier = Modifier.padding(20.dp),
            text = "----------------     or     ----------------",
            fontSize = 20.sp,
            color = Color.Gray
        )

        Row(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(
                onClick = {
                    onSignOut()
                    navigateToLogin()
                },
                enabled = enabled2
            ) {
                Text("Sign out and try again")
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

