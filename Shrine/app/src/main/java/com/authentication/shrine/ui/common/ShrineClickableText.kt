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

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * A custom Clickable Text that can be reused throughout the project
 *
 * @param text Displayed text
 * @param clickableText Part of the text that needs to be clickable
 * @param onTextClick onclick lambda for the clickable text
 * @param textStyle Text style for the test
 * @param modifier The [Modifier] to be applied to the ClickableText
 * */
@Composable
fun ShrineClickableText(
    text: String,
    clickableText: String,
    onTextClick: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val annotatedText = buildAnnotatedString {
        append(text)
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            pushStringAnnotation("url", "url")
            append(clickableText)
        }
    }

    ClickableText(
        modifier = modifier,
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "url", start = offset, end = offset).firstOrNull()?.let {
                onTextClick()
            }
        },
        style = textStyle,
    )
}
