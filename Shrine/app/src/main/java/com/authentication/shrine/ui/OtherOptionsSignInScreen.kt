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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * Composable function that displays the screen for non-passkey registration options.
 *
 * @param onSignUpWithPasswordClicked Callback for signing up with password.
 */
@Composable
fun OtherOptionsSignInScreen(
    onSignUpWithPasswordClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.padding_medium)),
        ) {
            ShrineToolbar(
                showBack = true,
                onBackClicked = onBackClicked,
            )
            ShrineTextHeader(
                text = stringResource(R.string.other_options),
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_large))) // Spacer like in RegisterScreen
            ShrineButton(
                onClick = onSignUpWithPasswordClicked,
                buttonText = stringResource(R.string.sign_up_with_password),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OtherOptionsSignInScreenPreview() {
    ShrineTheme {
        OtherOptionsSignInScreen(
            onBackClicked = {},
            onSignUpWithPasswordClicked = {},
        )
    }
}
