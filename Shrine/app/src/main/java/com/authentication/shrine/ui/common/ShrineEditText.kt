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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.grayBackground

@Composable
fun ShrineEditText(
    title: String,
    text: String = "",
    isFieldLocked: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
    ) {
        Text(text = title)

        OutlinedTextField(
            value = text,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth(),
            readOnly = isFieldLocked,
            shape = RoundedCornerShape(dimensionResource(R.dimen.size_standard)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Red,
                focusedContainerColor = grayBackground,
                unfocusedContainerColor = grayBackground,
                disabledContainerColor = grayBackground,
                errorContainerColor = grayBackground,
            ),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun ShrineEditTextPreview() {
    ShrineTheme {
        ShrineEditText(
            "Full Name",
            "ABC XYZ",
        )
    }
}
