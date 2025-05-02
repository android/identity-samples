package com.androidauth.shrineWear.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.R

@Composable
fun OAuthScreen(
    oAuthViewModel: OAuthViewModel,
    navigateToSignOut: () -> Unit,
    navigateToLegacyLogin: () -> Unit,
) {
    val uiState by oAuthViewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(key1 = uiState.statusCode) {
        if (uiState.statusCode == R.string.status_authorized) {
            navigateToSignOut()
        }
    }

    ScreenScaffold {
        ScalingLazyColumn(state = listState) {
            item {
                ListHeader {
                    Text(
                        stringResource(R.string.oauth_login_screen),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Button(
                    onClick = { oAuthViewModel.signInWithOauth() },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = stringResource(R.string.authorize_device),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
            item { Text(stringResource(id = uiState.statusCode)) }
            item { Text(uiState.resultMessage) }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigateToLegacyLogin() },
                    label = { Text(text = "Cancel") },
                    secondaryLabel = { Text(text = "Back to Legacy Options")}
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun OAuthScreenPreview() {
    OAuthScreen(
        oAuthViewModel = OAuthViewModel(application = Application()),
        navigateToSignOut = {}, navigateToLegacyLogin = {}
    )
}