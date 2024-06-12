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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.ui.theme.ShrineTheme

/**
 * A composable that displays the top menu for the Shrine app.
 *
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun TopMenu(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.ic_menu_24px),
            contentDescription = stringResource(R.string.shrine_app_menu_mockup),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = stringResource(R.string.shrine_app_menu_mockup),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.ic_search_24px),
            contentDescription = stringResource(R.string.shrine_app_menu_mockup),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(dimensionResource(R.dimen.dimen_4)),
        )

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.dimen_12)))

        Image(
            painter = painterResource(R.drawable.image),
            contentDescription = stringResource(R.string.shrine_app_menu_mockup),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(dimensionResource(R.dimen.dimen_4)),
        )
    }
}

/**
 * A preview of the TopMenu composable.
 */
@Preview(showBackground = true)
@Composable
fun TopMenuPreview() {
    ShrineTheme {
        TopMenu()
    }
}
