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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import com.google.credentialmanager.sample.Graph
import com.google.credentialmanager.sample.ui.viewmodel.AuthUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

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

    Scaffold(
        bottomBar = {
            BottomAppBar(
                fabShape = RoundedCornerShape(50),
                onSignOut = {
                    onSignOut()
                    navigateToLogin()
                },
                currentEmail = "",
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(20.dp), text = "Home Screen", fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(20.dp),
                text = "This is a sample app to help integrate & test Credential Manager apis.",
                fontSize = 20.sp
            )

            Row(
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Button(onClick = {
                    //Fetch credentials for your account
                    onRegisterRequest()
                }) {
                    Text("Create a passkey")
                }
            }
            //Handle UiState values
            when (uiState) {
                is HomeUiState.Empty -> {}

                is HomeUiState.CreationResult -> coroutineScope.launch {
                    val data = auth.createPasskey(activity, uiState.data)
                    if (data != null) {
                        onRegisterResponse(data)
                    } else {
                        Toast.makeText(
                            activity,
                            "Some error occurred, Please try registering again",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                is HomeUiState.MsgString -> {
                    LaunchedEffect(uiState) {
                        Toast.makeText(activity, uiState.msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

@Composable
fun BottomAppBar(
    fabShape: RoundedCornerShape, onSignOut: () -> Unit, currentEmail: String
) {
    BottomAppBar(
        elevation = 40.dp, cutoutShape = fabShape
    ) {
        Text(
            modifier = Modifier.padding(start = 10.dp), text = currentEmail
        )
        Spacer(Modifier.weight(1f, true))
        IconButton(onClick = {
            onSignOut()
        }) {
            Icon(Filled.Logout, "Sign Out")
        }
    }
}

