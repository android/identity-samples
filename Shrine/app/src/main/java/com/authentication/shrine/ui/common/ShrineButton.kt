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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.dark_button
import com.authentication.shrine.ui.theme.light_button

/**
 * The default shape for Shrine buttons.
 */
private val ButtonShape = RoundedCornerShape(50)

/**
 * A custom button composable for the Shrine app.
 *
 * @param onClick The callback to be invoked when the button is clicked.
 * @param buttonText Text to be displayed on the button
 * @param modifier The modifier to be applied to the button.
 * @param isButtonEnabled Whether the button is enabled.
 * @param shape The shape of the button.
 * @param border The border of the button.
 * @param interactionSource The interaction source for the button.
 */
@Composable
fun ShrineButton(
    onClick: () -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
    isButtonDark: Boolean = true,
    isButtonEnabled: Boolean = true,
    shape: Shape = ButtonShape,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = ButtonColors(
            containerColor = if (isButtonDark) {
                dark_button
            } else {
                light_button
            },
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
            disabledContentColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        enabled = isButtonEnabled,
        border = border,
        interactionSource = interactionSource,
    ) {
        Text(
            text = buttonText,
            color = if (isButtonDark) {
                light_button
            } else {
                dark_button
            },
        )
    }
}

/**
 * A preview of the ShrineButton composable.
 */
@Preview
@Composable
private fun ButtonPreview() {
    ShrineTheme {
        ShrineButton(
            onClick = { },
            buttonText = stringResource(R.string.demo),
        )
    }
}
