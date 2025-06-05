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
package com.authentication.shrine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale.Companion
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.authentication.shrine.R
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.ui.common.ShrineClickableText
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.grayBackground
import com.authentication.shrine.ui.viewmodel.SettingsViewModel
import com.authentication.shrine.utility.toImageSvgString
import com.authentication.shrine.utility.toReadableDate
import kotlinx.coroutines.Dispatchers

/**
 * Stateful composable of the Passkeys Management Screen
 *
 * @param onLearnMoreClicked onclick lambda invoked when clicked on learn more about passkeys
 * @param viewModel [SettingsViewModel] passed with the same data from the Settings Screen
 * @param modifier Modifier to modify the UI of the screen
 * */
@Composable
fun PasskeyManagementScreen(
    onLearnMoreClicked: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState.collectAsState().value

    PasskeyManagementScreen(
        onLearnMoreClicked = onLearnMoreClicked,
        passkeysList = uiState.passkeysList,
        modifier = modifier,
    )
}

/**
 * Stateless composable of the Passkey Management Screen
 *
 * @param onLearnMoreClicked onclick lambda invoked when clicked on learn more about passkeys
 * @param passkeysList List of [PasskeyCredential] from the [SettingsViewModel]
 * @param modifier Modifier to modify the UI of the screen
 * */
@Composable
fun PasskeyManagementScreen(
    onLearnMoreClicked: () -> Unit,
    passkeysList: List<PasskeyCredential>,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_standard)),
        ) {
            ShrineToolbar(true)

            ShrineTextHeader(stringResource(R.string.passkeys))

            ShrineClickableText(
                text = stringResource(R.string.passkeys_info),
                clickableText = stringResource(R.string.learn_more_about_passkeys),
                onTextClick = onLearnMoreClicked,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small)),
            )

            LazyColumn(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.padding_small))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.padding_small)))
                    .background(grayBackground)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .fillMaxWidth(),
            ) {
                itemsIndexed(
                    items = passkeysList,
                    itemContent = { index, item ->
                        PasskeysDetailsRow(
                            iconUrl = item.providerIcon,
                            credentialProviderName = item.name,
                            passkeyCreationDate = item.registeredAt.toReadableDate(),
                        )

                        if (index < passkeysList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_extra_small), horizontal = dimensionResource(R.dimen.dimen_standard)),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Composable to display the list of Passkeys
 *
 * @param iconUrl Icon URL fetched from the server for the credential provider
 * @param credentialProviderName Name of the credential provider for the passkey
 * @param passkeyCreationDate Date when the passkey was created
 * */
@Composable
fun PasskeysDetailsRow(
    iconUrl: String,
    credentialProviderName: String,
    passkeyCreationDate: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dimen_standard)),
    ) {
        AsyncImage(
            modifier = Modifier.size(48.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconUrl.toImageSvgString() ?: R.drawable.ic_passkey)
                .decoderFactory(SvgDecoder.Factory())
                .decoderDispatcher(Dispatchers.IO)
                .build(),
            contentScale = Companion.Inside,
            contentDescription = stringResource(R.string.credential_provider_logo),
        )

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
        ) {
            Text(
                text = credentialProviderName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.created, passkeyCreationDate),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            text = stringResource(R.string.delete),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Preview of the stateless composable of the passkey management screen
 * */
@Preview(showBackground = true)
@Composable
fun PasskeyManagementScreenPreview() {
    ShrineTheme {
        PasskeyManagementScreen(
            onLearnMoreClicked = { },
            passkeysList = listOf(),
        )
    }
}
