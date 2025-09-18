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

/**
 * A stateful Composable screen that allows users to update their profile information,
 * specifically their username and display name.
 *
 * @param onBackClicked Lambda to be invoked when the back button in the toolbar is clicked.
 * @param viewModel An instance of [UpdateProfileViewModel] used to handle the business logic
 */
@Composable
fun UpdateProfileScreen(
    onBackClicked: () -> Unit,
    viewModel: UpdateProfileViewModel,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    UpdateProfileScreen(
        username = username,
        onUsernameChanged = { username = it },
        displayName = email,
        onDisplayNameChanged = { email = it },
        onMetadataUpdate = viewModel::updateMetadata,
        onBackClicked = onBackClicked,
    )
}

/**
 * A stateless Composable screen that provides the UI for updating user profile information.
 *
 * @param username The current value of the username to be displayed in the text field.
 * @param onUsernameChanged Lambda to update the username on change.
 * @param displayName The current value of the display name to be displayed in the text field.
 * @param onDisplayNameChanged Lambda to update the display name on change
 * @param onMetadataUpdate Lambda function that is invoked when the update button is clicked.
 *                         It receives the current username and display name strings as parameters,
 * @param onBackClicked Lambda function to be invoked when the back button in the toolbar is clicked
 */
@Composable
fun UpdateProfileScreen(
    username: String,
    onUsernameChanged: (String) -> Unit,
    displayName: String,
    onDisplayNameChanged: (String) -> Unit,
    onMetadataUpdate: (String, String) -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ShrineToolbar(
            onBackClicked = onBackClicked
        )

        ShrineTextField(
            value = username,
            onValueChange = onUsernameChanged,
            hint = stringResource(R.string.username),
            modifier = Modifier.fillMaxWidth(),
        )

        ShrineTextField(
            value = displayName,
            onValueChange = onDisplayNameChanged,
            hint = stringResource(R.string.display_name),
            modifier = Modifier.fillMaxWidth(),
        )

        ShrineButton(
            onClick = { onMetadataUpdate(username, displayName) },
            buttonText = stringResource(R.string.update_user_info),
        )
    }
}

/**
 * Preview Composable function of [UpdateProfileScreen]
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TestPreview() {
    ShrineTheme {
        UpdateProfileScreen(
            username = "",
            onUsernameChanged = { },
            displayName = "",
            onDisplayNameChanged = { },
            onMetadataUpdate = { _, _ -> },
            onBackClicked = { }
        )
    }
}
