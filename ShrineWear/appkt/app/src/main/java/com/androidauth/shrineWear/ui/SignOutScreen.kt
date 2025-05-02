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
        show = showDialog,
        onDismissRequest = { signOut() },
        edgeButton = { AlertDialogDefaults.EdgeButton(onClick = { signOut() }) },
        title = { Text(text = "You are now signed in", textAlign = TextAlign.Center) },
        text = { Text("To sign out and start over, tap the check mark") },
    )
}

@WearPreviewDevices
@Composable
fun SignOutScreenPreview() {
    SignOutScreen(navigateToHome = {})
}