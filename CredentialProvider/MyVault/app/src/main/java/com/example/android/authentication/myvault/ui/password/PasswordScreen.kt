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
package com.example.android.authentication.myvault.ui.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.authentication.myvault.Dimensions
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.ui.theme.MyVaultTheme

/**
 * This stateful composable holds the state values to be passed to the UI displayed to user while saving/updating the password credential for selected site
 *
 * @param onSave The callback to be invoked when the user clicks the "Save" button
 * @param modifier The modifier to be applied to the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PasswordScreen(
        onSave,
        bottomSheetState,
        modifier,
    )
}

/**
 * This stateless composable is for the UI displayed to user while saving/updating the password credential for selected site
 *
 * @param onSave The callback to be invoked when the user clicks the "Save" button
 * @param bottomSheetState The state of the bottom sheet
 * @param modifier The modifier to be applied to the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(
    onSave: () -> Unit,
    bottomSheetState: SheetState,
    modifier: Modifier = Modifier,
) {
    var showBottomSheet by rememberSaveable { mutableStateOf(true) }

    MyVaultTheme {
        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = modifier,
                sheetState = bottomSheetState,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                content = {
                    Card(
                        modifier = Modifier
                            .heightIn(min = 20.dp)
                            .padding(Dimensions.padding_medium)
                            .background(MaterialTheme.colorScheme.onPrimary),
                    ) {
                        SaveYourPasswordCard(onSave, Modifier)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                onDismissRequest = {
                    showBottomSheet = false
                },
            )
        }
    }
}

@Composable
private fun SaveYourPasswordCard(onSave: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.onPrimary)) {
        Icon(
            painter = painterResource(R.drawable.android_secure),
            contentDescription = null,
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally)
                .size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.save_your_password_to_vault),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Dimensions.padding_medium),
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.padding_extra_large)
                .align(Alignment.End),
            onClick = onSave,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.text_continue),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/**
 * This composable function provides a preview of the PasswordScreen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PasswordScreenPreview() {
    PasswordScreen(
        onSave = { },
        bottomSheetState = rememberModalBottomSheetState(false),
        modifier = Modifier,
    )
}
