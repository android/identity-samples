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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.theme.ShrineTheme

/**
 * A composable that displays information about passkeys.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param onLearnMoreClicked The callback to be invoked when the "Learn More" text is clicked.
 */
@Composable
fun PasskeyInfo(
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.passkey_info_heading),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = dimensionResource(R.dimen.dimen_24)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.dimen_16)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.passkey_info),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = Dimensions.TEXT_SMALL,
                        lineHeight = Dimensions.SMALL_LINE_HEIGHT,
                    ),
                )
                Text(
                    text = stringResource(R.string.learn_more),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.dimen_12))
                        .clickable { onLearnMoreClicked() },
                )
            }

            Image(
                painter = painterResource(R.drawable.passkey_image),
                contentDescription = stringResource(R.string.passkey_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(dimensionResource(R.dimen.dimen_172)),
            )
        }
    }
}

/**
 * A preview of the PasskeyInfo composable.
 */
@Preview(showBackground = true)
@Composable
fun PasskeyInfoPreview() {
    ShrineTheme {
        PasskeyInfo(
            onLearnMoreClicked = { },
        )
    }
}
