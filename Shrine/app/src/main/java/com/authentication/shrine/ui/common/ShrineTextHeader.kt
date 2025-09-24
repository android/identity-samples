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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * A composable that displays a text header.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param text The text to be displayed.
 */
@Composable
fun ShrineTextHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_small)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

/**
 * A preview of the TextHeader composable.
 */
@Preview(showBackground = true)
@Composable
fun ShrineTextHeaderPreview() {
    ShrineTheme {
        ShrineTextHeader(text = stringResource(R.string.heading_text))
    }
}
