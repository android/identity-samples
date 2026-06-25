/*
 * Copyright 2026 Google LLC
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

package com.example.digitalcredentials

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Stateful entry point for the Main screen.
 *
 * This composable initializes the [MainViewModel] and provides credential retrieval
 * logic to the ViewModel via suspend lambdas.
 *
 * @param modifier The modifier to be applied to the screen.
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    MainContent(
        uiState = uiState,
        onGetDigitalCredentialClick = {
            val activity = context.findActivity()
            if (activity != null) {
                viewModel.getDigitalCredential {
                    CredentialManagerUtil.getDigitalCredential(activity)
                }
            }
        },
        onGetVerifiedEmailClick = {
            val activity = context.findActivity()
            if (activity != null) {
                viewModel.getVerifiedEmailCredential {
                    CredentialManagerUtil.getVerifiedEmailCredential(activity)
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless UI content for the Main screen.
 *
 * Displays the credential request actions and the resulting claims in a clean layout.
 *
 * @param uiState The current state of the UI.
 * @param onGetDigitalCredentialClick Callback triggered when the Digital Credential button is clicked.
 * @param onGetVerifiedEmailClick Callback triggered when the Verified Email button is clicked.
 * @param modifier The modifier to be applied to the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    uiState: MainUiState,
    onGetDigitalCredentialClick: () -> Unit,
    onGetVerifiedEmailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.header_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onGetDigitalCredentialClick,
                enabled = uiState !is MainUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 8.dp)
            ) {
                Text(text = stringResource(R.string.get_digital_credential))
            }

            Button(
                onClick = onGetVerifiedEmailClick,
                enabled = uiState !is MainUiState.Loading,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = stringResource(R.string.get_verified_email))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ResultSection(uiState)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Renders the results of a credential request based on the current [MainUiState].
 *
 * @param uiState The state to render.
 */
@Composable
fun ResultSection(uiState: MainUiState) {
    when (uiState) {
        is MainUiState.Initial -> {
            Text(
                text = stringResource(R.string.results_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        is MainUiState.Loading -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.requesting_credential),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        is MainUiState.Success -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                uiState.claims.forEach { claim ->
                    ClaimCard(claim)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (uiState.claims.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_claims_extracted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is MainUiState.Error -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Displays an individual [CredentialClaim] in a card.
 *
 * @param claim The claim to display.
 */
@Composable
fun ClaimCard(claim: CredentialClaim) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = claim.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = claim.value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainContent(
            uiState = MainUiState.Success(
                title = "Driver's License",
                claims = listOf(
                    CredentialClaim("Given Name", "John"),
                    CredentialClaim("Family Name", "Doe"),
                    CredentialClaim("Age Over 21", "Yes")
                )
            ),
            onGetDigitalCredentialClick = {},
            onGetVerifiedEmailClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenLoadingPreview() {
    MaterialTheme {
        MainContent(
            uiState = MainUiState.Loading,
            onGetDigitalCredentialClick = {},
            onGetVerifiedEmailClick = {}
        )
    }
}
