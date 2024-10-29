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
package com.example.android.authentication.shrine.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.theme.ShrineTheme

/**
 * The Security section of the Settings screen, providing options for managing security settings
 * such as creating passkeys and changing passwords.
 *
 * @param onCreatePasskeyClicked Callback to be invoked when the "Create Passkey" button is clicked.
 * @param onChangePasswordClicked Callback to be invoked when the "Change" text in the
 * Change Password component is clicked.
 * @param modifier Modifier to be applied to the Security section.
 */
@Composable
fun SecuritySection(
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(dimensionResource(R.dimen.padding_small)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.security),
            style = TextStyle(textAlign = TextAlign.Start),
            modifier = Modifier
                .padding(bottom = dimensionResource(R.dimen.padding_medium))
                .fillMaxWidth(),
        )

        PasskeyInfo(
            onLearnMoreClicked = onLearnMoreClicked,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_minimum)))

        ShrineButton(
            onClick = onCreatePasskeyClicked,
            buttonText = stringResource(R.string.create_passkey),
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_minimum)))

        ChangePasswordSection(
            onChangePasswordClicked = onChangePasswordClicked,
            modifier = modifier
                .height(72.dp)
                .background(color = MaterialTheme.colorScheme.surfaceContainer),
        )
    }
}

/**
 * Preview of the Security section.
 */
@Preview(showBackground = true)
@Composable
fun SecuritySectionPreview() {
    ShrineTheme {
        SecuritySection(
            onCreatePasskeyClicked = { },
            onChangePasswordClicked = { },
            onLearnMoreClicked = { },
        )
    }
}
