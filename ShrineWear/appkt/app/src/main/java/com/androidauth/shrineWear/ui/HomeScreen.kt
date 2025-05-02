package com.androidauth.shrineWear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.Graph
import com.androidauth.shrineWear.R

@Composable
fun HomeScreen(
    credentialManagerViewModel: CredentialManagerViewModel,
    navigateToLegacyLogin: () -> Unit,
    navigateToSignOut: () -> Unit
) {
    val uiState by credentialManagerViewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (!uiState.inProgress) {
        when (Graph.authenticationStatusCode) {
            R.string.credman_status_logged_out -> {
                credentialManagerViewModel.login(context)
            }
            R.string.credman_status_dismissed, R.string.credman_status_no_credentials -> {
                navigateToLegacyLogin()
            }
            R.string.credman_status_authorized -> {
                navigateToSignOut()
            }
            else -> {
                // Temporary while I solve the passkey issue
                navigateToLegacyLogin()
            }
        }
    } else {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    }
}

@WearPreviewDevices
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        credentialManagerViewModel = CredentialManagerViewModel(),
        navigateToLegacyLogin = {},
        navigateToSignOut = {},
    )
}