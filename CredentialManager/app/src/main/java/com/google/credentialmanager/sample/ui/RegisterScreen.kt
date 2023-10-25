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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import com.google.credentialmanager.sample.Graph
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.common.TextHeader
import com.google.credentialmanager.sample.ui.viewmodel.AuthUiState
import com.google.credentialmanager.sample.ui.viewmodel.AuthenticationViewModel

@Composable
fun RegisterRoute(
    navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit,
    viewModel: AuthenticationViewModel
) {
    val uiState = viewModel.uiState.collectAsState().value

    RegisterScreen(
        navigateToHome,
        viewModel::login,
        viewModel::signInRequest,
        viewModel::signInResponse,
        uiState
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun RegisterScreen(
    navigateToHome: (flag: Boolean) -> Unit,
    onLogin: (String, String) -> Unit,
    onSignInRequest: () -> Unit,
    onSignInResponse: (GetCredentialResponse) -> Unit,
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
            .padding(20.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextHeader(text = stringResource(R.string.create_account))
        Spacer(Modifier.padding(20.dp))
        Column(
            modifier = Modifier
                .background(
                    color = Color(0xFFF9F2F1),
                )
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                modifier = Modifier.padding(top = 16.dp),
                value = email,
                leadingIcon = { Icon(imageVector = Icons.Filled.Email, contentDescription = "emailIcon") },
                singleLine = true,
                //isError = isEmailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                onValueChange = { email = it},
                label = { Text("E-mail address") },
                placeholder = { Text("E-mail address") }

            )
            TextField(
                modifier = Modifier.padding(top = 16.dp),
                value = password,
                leadingIcon = { Icon(imageVector = Icons.Filled.Password, contentDescription = "passwordIcon" ) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                onValueChange = { password = it },
                label = { Text("Password") },
                trailingIcon = {
                    val image = if (passwordVisible) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    }
                    IconButton(onClick = {passwordVisible = !passwordVisible}){
                        Icon(imageVector  = image, "password")
                    }
                }
            )
            Spacer(Modifier.padding(20.dp))
            ShrineButton(
                onClick = {
                    if ( email.isNotEmpty() && password.isNotEmpty()
                    ) {
                        Toast.makeText(
                            activity, "Wait, signing you in.", Toast.LENGTH_SHORT
                        ).show()

                        onLogin(email, password)
                    } else {
                        Toast.makeText(
                            activity, "Enter valid username and password", Toast.LENGTH_LONG
                        ).show()
                    }
                },
                Modifier.widthIn(min = 280.dp),
                enabled = enabled1
                ) {
                Text(text = stringResource(R.string.sign_up))
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
}