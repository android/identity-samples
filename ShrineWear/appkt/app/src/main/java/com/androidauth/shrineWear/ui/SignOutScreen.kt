/*
 * Copyright 2024 The Android Open Source Project
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
package com.androidauth.shrineWear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.Graph
import com.androidauth.shrineWear.R

/**
 * Composable screen displayed after a successful sign-in, allowing the user to sign out.
 *
 * This screen presents an [AlertDialog] confirming the sign-in status and provides an
 * option to sign out and return to the home screen.
 *
 * @param navigateToHome A lambda function to navigate back to the home screen after sign-out.
 */
@Composable
fun SignOutScreen(
    navigateToHome: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(true) }

    fun signOut() {
        Graph.authenticationStatusCode = R.string.credman_status_logged_out
        Graph.credentialManagerAuthenticator.signOut()
        showDialog = false
        navigateToHome()
    }

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { signOut() },
        edgeButton = { AlertDialogDefaults.EdgeButton(onClick = { signOut() }) },
        title = { Text(text = "You are now signed in", textAlign = TextAlign.Center) },
        text = { Text("To sign out and start over, tap the check mark") },
    )
}

/**
 * Preview for the [SignOutScreen] composable.
 *
 * This preview renders the [SignOutScreen] with a dummy navigation action.
 */
@WearPreviewDevices
@Composable
fun SignOutScreenPreview() {
    SignOutScreen(navigateToHome = {})
}
