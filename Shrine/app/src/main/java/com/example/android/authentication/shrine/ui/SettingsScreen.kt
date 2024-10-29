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
package com.example.android.authentication.shrine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.common.ContactUsSection
import com.example.android.authentication.shrine.ui.common.SecuritySection
import com.example.android.authentication.shrine.ui.common.ShrineTextHeader
import com.example.android.authentication.shrine.ui.common.UsernameSection
import com.example.android.authentication.shrine.ui.theme.ShrineTheme

/**
 * Composable function that displays the settings screen.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param onCreatePasskeyClicked Callback for when the create passkey button is clicked.
 * @param onChangePasswordClicked Callback for when the change password button is clicked.
 * @param onHelpClicked Callback for when the help button is clicked.
 */
@Composable
fun SettingsScreen(
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShrineTextHeader(
            text = stringResource(R.string.account),
        )
        Image(
            painter = painterResource(R.drawable.person_24px),
            contentDescription = stringResource(R.string.password),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.width(dimensionResource(R.dimen.size_medium)),
        )

        UsernameSection(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.padding_small))
                .fillMaxWidth(),
        )

        SecuritySection(
            onCreatePasskeyClicked,
            onChangePasswordClicked,
            onLearnMoreClicked,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_large)))

        ContactUsSection(
            onHelpClicked = onHelpClicked,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
        )
    }
}

/**
 * Generates a preview of the SettingsScreen composable function.
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ShrineTheme {
        SettingsScreen(
            onCreatePasskeyClicked = { },
            onChangePasswordClicked = { },
            onHelpClicked = { },
            onLearnMoreClicked = { },
        )
    }
}
