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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.androidauth.shrineWear.R

/**
 * The main entry point composable for the Shrine Wear OS application.
 *
 * This composable sets up the navigation controller and actions,
 * then displays initial demo instructions before rendering the main navigation graph.
 */
@Composable
fun ShrineApp() {
    val navController = rememberSwipeDismissableNavController()
    val navigationActions = remember(navController) { ShrineNavActions(navController) }

    DemoInstructions()
    ShrineNavGraph(navController = navController, navigationActions = navigationActions)
}

/**
 * Displays an [AlertDialog] containing introductory demo instructions for the user.
 *
 * This dialog is shown upon the initial launch of the application and can be dismissed
 * by the user.
 *
 * Note: The `AlertDialog` API used here (`edgeButton` and `visible`) might be from an
 * older or specific alpha version of `androidx.wear.compose.material3`. For newer
 * versions, consider using `confirmButton` and `dismissButton` for actions, and
 * conditionally rendering the dialog using an `if` statement.
 */
@Composable
fun DemoInstructions() {
    var showDialog by remember { mutableStateOf(true) }

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        edgeButton = { AlertDialogDefaults.EdgeButton(onClick = { showDialog = false }) },
        title = {
            Text(
                text = stringResource(R.string.shrine_sample),
                textAlign = TextAlign.Center,
            )
        },
        text = { Text(stringResource(R.string.see_readme_md_for_usage_directions)) },
    )
}

/**
 * Preview for the [DemoInstructions] composable.
 *
 * This preview renders the dialog with the demo instructions as it would appear on Wear OS devices.
 */
@WearPreviewDevices
@Composable
fun DemoInstructionsPreview() {
    DemoInstructions()
}
