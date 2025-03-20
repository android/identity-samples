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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * A composable that displays the Shrine logo as a heading.
 *
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun LogoHeading(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.shrine_home_logo),
        contentDescription = stringResource(R.string.large_shrine_logo),
        modifier = modifier.size(152.dp),
    )
}

/**
 * A preview of the LogoHeading composable.
 */
@Preview(showBackground = true)
@Composable
fun WelcomeLogoPreview() {
    ShrineTheme {
        LogoHeading()
    }
}
