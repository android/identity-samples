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

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * A clickable "Learn More" text composable.
 *
 * @param modifier The modifier to be applied to the text.
 */
@Composable
fun ClickableLearnMore(
    modifier: Modifier = Modifier,
) {
    val passkeysVideoUrl = stringResource(R.string.passkeys_youtube_url)
    val context = LocalContext.current
    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(passkeysVideoUrl)) }
    TextButton(
        modifier = modifier,
        onClick = { context.startActivity(intent) },
    ) {
        Text(
            text = stringResource(R.string.learn_more),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * A preview of the ClickableLearnMore composable.
 */
@Preview(showBackground = true)
@Composable
fun ClickableLearnMorePreview() {
    ShrineTheme {
        ClickableLearnMore()
    }
}
