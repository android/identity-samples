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
package com.authentication.shrine.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.viewmodel.ErrorDialogUiState
import com.authentication.shrine.ui.viewmodel.ErrorDialogViewModel

@Composable
fun ErrorAlertDialog(
    errorMessage: String,
) {
    val viewModel: ErrorDialogViewModel = hiltViewModel()
    val errorDialogUiState = viewModel.uiState.collectAsState().value

    ErrorAlertDialog(
        errorMessage = errorMessage,
        hideAlert = viewModel::hideErrorDialog,
        uiState = errorDialogUiState,
    )
}

@Composable
fun ErrorAlertDialog(
    errorMessage: String,
    hideAlert: () -> Unit,
    uiState: ErrorDialogUiState,
) {
    if (uiState.showAlert) {
        AlertDialog(
            onDismissRequest = { hideAlert() },
            confirmButton = {
                Button(onClick = { hideAlert() }) {
                    Text(stringResource(R.string.dismiss))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = "",
                )
            },
            title = {
                Text(stringResource(R.string.error))
            },
            text = {
                Text(text = errorMessage)
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DialogPreview() {
    ShrineTheme {
        ErrorAlertDialog(
            errorMessage = "An error occurred",
            hideAlert = { },
            uiState = ErrorDialogUiState(),
        )
    }
}
