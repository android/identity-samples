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
package com.example.android.authentication.myvault.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.authentication.myvault.Dimensions
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.room.SiteWithCredentials

/**
 * This stateful composable holds the state values to pass into the CredentialsList Composable
 *
 * @param sites The list of sites with credentials.
 * @param iconMap The map of site URLs to their corresponding icons.
 * @param onSiteSelected The callback to be invoked when a site is selected.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun CredentialsList(
    sites: List<SiteWithCredentials>,
    iconMap: Map<String, Bitmap>,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.your_saved_credentials_appear_here),
            modifier = Modifier.padding(Dimensions.padding_large),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.padding_medium),
            shape = RoundedCornerShape(5),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy((-1).dp),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            ) {
                items(sites) {
                    CredentialEntry(
                        site = it,
                        iconMap[it.site.url],
                        onSiteSelected = onSiteSelected,
                    )
                }
            }
        }
    }
}

/**
 * This stateless composable is for all the credentials saved in MyVault through different client apps.
 *
 * @param sites The list of sites with credentials.
 * @param iconMap The map of site URLs to their corresponding icons.
 * @param onSiteSelected The callback to be invoked when a site is selected
 *  @param modifier The modifier to be applied to the composable.
 */
@Composable
fun CredentialEntry(
    site: SiteWithCredentials,
    icon: Bitmap?,
    onSiteSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.padding_medium),
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimensions.padding_small,
        ),
        onClick = { onSiteSelected(site.site.id) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            if (icon == null) {
                Image(
                    modifier = Modifier
                        .padding(Dimensions.padding_medium)
                        .size(Dimensions.padding_extra_large, Dimensions.padding_extra_large),
                    imageVector = Icons.Filled.Lock,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    contentDescription = stringResource(R.string.lock),
                )
            } else {
                Image(
                    modifier = Modifier
                        .padding(Dimensions.padding_medium)
                        .size(Dimensions.padding_extra_large, Dimensions.padding_extra_large),
                    bitmap = icon.asImageBitmap(),
                    contentDescription = site.site.name,
                )
            }
            Text(
                text = site.site.url,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * This composable function provides a preview of the CredentialsList composable.
 */
@Preview
@Composable
fun CredentialsListPreview() {
    val list: List<SiteWithCredentials> = emptyList()
    val iconMap = emptyMap<String, Bitmap>()

    CredentialsList(
        sites = list,
        iconMap = iconMap,
        onSiteSelected = {},
        modifier = Modifier,
    )
}
