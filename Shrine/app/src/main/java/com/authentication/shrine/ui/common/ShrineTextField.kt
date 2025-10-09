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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * A custom TextField composable for the Shrine app.
 *
 * @param title The current value of the TextField.
 * @param text The callback to be invoked when the TextField value changes.
 */
@Composable
fun ShrineTextField(
    title: String,
    text: String = "",
    enabled: Boolean = false,
    onValueChanged: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = text,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(dimensionResource(R.dimen.size_standard)),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            ),
        )
    }
}

/**
 * A preview of the ShrineTextField composable.
 */
@Preview(showSystemUi = true, name = "ShrineTextField Light")
@Composable
fun ShrineTextFieldPreviewLight() {
    ShrineTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShrineTextField(
                "Full Name",
                "ABC XYZ",
            )
        }
    }
}

@Preview(showSystemUi = true, name = "ShrineTextField Dark")
@Composable
fun ShrineTextFieldPreviewDark() {
    ShrineTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShrineTextField(
                "Full Name",
                "ABC XYZ",
            )
        }
    }
}
