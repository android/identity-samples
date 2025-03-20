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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.authentication.shrine.R
import com.authentication.shrine.ui.theme.ShrineTheme

/**
 * The Change Password component of the Settings screen, consisting of an icon,
 * password information, and a clickable "Change" text.
 *
 * @param onChangePasswordClicked Callback to be invoked when the "Change" text is clicked.
 * @param modifier Modifier to be applied to the Change Password component.
 */
@Composable
fun ChangePasswordSection(
    onChangePasswordClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.clip_path_group),
            contentDescription = stringResource(R.string.password),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_small))
                .width(dimensionResource(R.dimen.padding_medium)),
        )

        Row(
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.padding_small))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.password),
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_extra_small)))

                Text(
                    text = stringResource(R.string.last_changed_april_13_2023),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            TextButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.padding_small)),
                onClick = onChangePasswordClicked,
            ) {
                Text(
                    text = stringResource(R.string.change),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * Preview of the Change Password component.
 */
@Preview(showBackground = true)
@Composable
fun ChangePasswordComponentPreview() {
    ShrineTheme {
        ChangePasswordSection(
            onChangePasswordClicked = { },
        )
    }
}
