package com.androidauth.shrineWear.ui


import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.Graph
import com.androidauth.shrineWear.R

private const val TAG = "LegacyLoginScreen"

@Composable
fun LegacyLoginScreen(
    navigateToOAuth: () -> Unit,
    navigateToSignInWithGoogle: () -> Unit,
    navigateToHome: () -> Unit,
    isCredentialManagerGSI: Boolean
) {
    val listState = rememberScalingLazyListState()

    ScreenScaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState
        ) {
            item {
                ListHeader {
                    Text(
                        stringResource(R.string.legacy_login_options),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Button(
                    onClick = { navigateToOAuth() },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = stringResource(R.string.sign_in_with_oauth),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
            item {
                Button(
                    onClick = {
                        if (isCredentialManagerGSI) {
                            Log.w(TAG,
                                "Legacy Google Sign in not available on devices" +
                                     "running SDK 35+ with Credential Manager. Use Sign in with " +
                                     "Google via Credential Manager to sign in."
                            )
                        } else {
                            navigateToSignInWithGoogle()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Disabled on SDK>34",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = { Icon(Icons.Filled.AccountCircle, "SiWG Icon") }
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        Graph.authenticationStatusCode = R.string.credman_status_logged_out
                        navigateToHome()
                    },
                    label = {
                        Text(
                            text = "Cancel",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Back to Home Screen",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun LegacyLoginScreenPreview() {
    LegacyLoginScreen(
        navigateToOAuth = {}, navigateToSignInWithGoogle = {}, navigateToHome = {},
        isCredentialManagerGSI = true
    )
}