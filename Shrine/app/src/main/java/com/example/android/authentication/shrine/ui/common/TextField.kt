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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.theme.ShrineTheme

/**
 * A custom TextField composable for the Shrine app.
 *
 * @param modifier The modifier to be applied to the TextField.
 * @param value The current value of the TextField.
 * @param onValueChange The callback to be invoked when the TextField value changes.
 */
@Composable
fun ShrineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .width(dimensionResource(R.dimen.dimen_300))
            .background(MaterialTheme.colorScheme.tertiaryContainer) // Set background color here
            .padding(dimensionResource(R.dimen.dimen_16)),
    )
}

/**
 * A preview of the ShrineTextField composable.
 */
@Preview
@Composable
private fun TextFieldPreview() {
    ShrineTheme {
        ShrineTextField(
            value = TextFieldValue(stringResource(R.string.demo)),
            onValueChange = { },
        )
    }
}
