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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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

        val activity = context.findActivity()!!

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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Consider using theme color: color = MaterialTheme.colorScheme.primary
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.operation_in_progress))
                }
            }

            signInError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.signIn {
                        getCredential(activity, it)
                    }
                },
                shape = RoundedCornerShape(4.dp),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_in_with_passkey_saved_password))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    CredentialManagerSampleTheme {
        SignInScreen(navController = rememberNavController())
    }
}
