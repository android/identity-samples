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
package com.authentication.shrine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ClickableLearnMore
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * Composable function for the learn more screen.
 *
 * This screen provides information about passkeys and their benefits.
 *
 * @param modifier Modifier to be applied to the composable.
 */
@Composable
fun LearnMoreScreen(
    modifier: Modifier = Modifier,
    onBackButtonClicked: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.passkey_image),
            contentDescription = stringResource(R.string.passkey_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(dimensionResource(R.dimen.size_extra_large))
                .align(Alignment.CenterHorizontally),
        )

        Text(
            text = stringResource(R.string.learn_more_heading),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
        )

        Text(
            text = stringResource(R.string.learn_more_line1),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
        )

        Text(
            text = stringResource(R.string.learn_more_line2),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_minimum)),
        )

        Text(
            text = stringResource(R.string.learn_more_line3),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_minimum)),
        )

        Text(
            text = stringResource(R.string.learn_more_line4),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_minimum)),
        )

        Text(
            text = stringResource(R.string.learn_more_line5),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_minimum)),
        )

        Text(
            text = stringResource(R.string.learn_more_line6),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small), vertical = dimensionResource(R.dimen.padding_minimum)),
        )

        ClickableLearnMore()

        Spacer(Modifier.height(dimensionResource(R.dimen.dimen_standard)))

        ShrineButton(
            onClick = onBackButtonClicked,
            buttonText = stringResource(R.string.back_button),
        )
    }
}

/**
 * Preview function for the learn more screen.
 *
 * This function displays a preview of the learn more screen in the Android Studio preview pane.
 */
@Preview(showBackground = true)
@Composable
fun LearnMoreScreenPreview() {
    ShrineTheme {
        LearnMoreScreen()
    }
}
