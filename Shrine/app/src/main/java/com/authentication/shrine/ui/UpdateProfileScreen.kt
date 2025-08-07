/*
 * Copyright 2025 The Android Open Source Project
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
package com.authentication.shrine.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineTextField
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.viewmodel.UpdateProfileViewModel

@Composable
fun UpdateProfileScreen(
    viewModel: UpdateProfileViewModel,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    UpdateProfileScreen(
        username = username,
        onUsernameChanged = { username = it },
        email = email,
        onEmailChanged = { email = it },
        onMetadataUpdate = viewModel::updateMetadata,
    )
}

@Composable
fun UpdateProfileScreen(
    username: String,
    onUsernameChanged: (String) -> Unit,
    email: String,
    onEmailChanged: (String) -> Unit,
    onMetadataUpdate: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ShrineToolbar(onBackClicked = {})

        ShrineTextField(
            value = username,
            onValueChange = onUsernameChanged,
            hint = stringResource(R.string.username),
            modifier = Modifier.fillMaxWidth()
        )

        ShrineTextField(
            value = email,
            onValueChange = onEmailChanged,
            hint = stringResource(R.string.email_address),
            modifier = Modifier.fillMaxWidth()
        )

        ShrineButton(
            onClick = { onMetadataUpdate(username, email) },
            buttonText = stringResource(R.string.update_user_info),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TestPreview() {
    ShrineTheme {
        UpdateProfileScreen(
            "",
            {},
            "",
            {},
            {_, _ -> },
        )
    }
}
