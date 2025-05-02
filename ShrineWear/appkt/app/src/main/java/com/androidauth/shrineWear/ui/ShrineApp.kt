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
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun ShrineApp() {
    val navController = rememberSwipeDismissableNavController()
    val navigationActions = remember(navController) { ShrineNavActions(navController) }

    DemoInstructions()
    ShrineNavGraph(navController = navController, navigationActions = navigationActions)
}

@Composable
fun DemoInstructions() {
    var showDialog by remember { mutableStateOf(true) }

    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        edgeButton = { AlertDialogDefaults.EdgeButton(onClick = { showDialog = false }) },
        title = {
            Text(
                text = "Shrine Sample",
                textAlign = TextAlign.Center,
            )
        },
        text = { Text("See readme.md for usage directions.") },
    )
}

@WearPreviewDevices
@Composable
fun DemoInstructionsPreview() {
    DemoInstructions()
}