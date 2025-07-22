/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.authentication.shrine.R
import com.authentication.shrine.ui.common.ShrineButton
import com.authentication.shrine.ui.common.ShrineClickableText
import com.authentication.shrine.ui.common.ShrineEditText
import com.authentication.shrine.ui.common.ShrineLoader
import com.authentication.shrine.ui.common.ShrineTextHeader
import com.authentication.shrine.ui.common.ShrineToolbar
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.theme.grayBackground
import com.authentication.shrine.ui.viewmodel.SettingsUiState
import com.authentication.shrine.ui.viewmodel.SettingsViewModel

/**
 * Stateful Composable for the Settings Screen
 *
 * @param viewModel [SettingsViewModel]
 * @param onCreatePasskeyClicked Lambda to create a passkey
 * @param onChangePasswordClicked Lambda invoked when change password is clicked
 * @param onLearnMoreClicked Lambda invoked when Learn more about passkeys is clicked
 * @param onManagePasskeysClicked Lambda to be invoked when manage passkeys is clicked
 * */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsState().value

    // Refresh data whenever screen is shown.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.getPasskeysList()
        }
    }

    SettingsScreen(
        onLearnMoreClicked = onLearnMoreClicked,
        onCreatePasskeyClicked = onCreatePasskeyClicked,
        onChangePasswordClicked = onChangePasswordClicked,
        onManagePasskeysClicked = onManagePasskeysClicked,
        onBackClicked = onBackClicked,
        uiState = uiState,
    )
}

/**
 * Stateless composable of the Settings Screen
 *
 * @param onCreatePasskeyClicked Lambda to create a passkey
 * @param onChangePasswordClicked Lambda invoked when change password is clicked
 * @param onLearnMoreClicked Lambda invoked when Learn more about passkeys is clicked
 * @param onManagePasskeysClicked Lambda to be invoked when manage passkeys is clicked
 * @param uiState [SettingsUiState] Holding the data to update the Composable
 * @param modifier [Modifier] responsible for formatting the Screen
 * */
@Composable
fun SettingsScreen(
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
    onBackClicked: () -> Unit,
    uiState: SettingsUiState,
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
            ShrineToolbar(
                showBack = true,
                onBackClicked = onBackClicked,
            )

            ShrineTextHeader(stringResource(R.string.account))

            ShrineEditText(
                title = stringResource(R.string.full_name),
                text = uiState.username,
                isFieldLocked = true,
            )

            ShrineEditText(
                title = stringResource(R.string.username),
                text = uiState.username,
                isFieldLocked = true,
            )

            SecuritySection(
                onLearnMoreClicked = onLearnMoreClicked,
                onCreatePasskeyClicked = onCreatePasskeyClicked,
                onChangePasswordClicked = onChangePasswordClicked,
                onManagePasskeysClicked = onManagePasskeysClicked,
                uiState = uiState,
            )
        }

        if (uiState.isLoading) {
            ShrineLoader()
        }

        val snackbarMessage = when {
            !uiState.errorMessage.isNullOrBlank() -> uiState.errorMessage
            uiState.messageResourceId != R.string.empty_string -> stringResource(uiState.messageResourceId)
            else -> null
        }

        if (snackbarMessage != null) {
            LaunchedEffect(snackbarMessage) {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage
                )
            }
        }
    }
}

/**
 * Composable Element for the Security section of the screen
 *
 * @param onCreatePasskeyClicked Lambda to create a passkey
 * @param onChangePasswordClicked Lambda invoked when change password is clicked
 * @param onLearnMoreClicked Lambda invoked when Learn more about passkeys is clicked
 * @param onManagePasskeysClicked Lambda to be invoked when manage passkeys is clicked
 * @param uiState [SettingsUiState] Holding the data to update the Composable
 * */
@Composable
fun SecuritySection(
    onLearnMoreClicked: () -> Unit,
    onCreatePasskeyClicked: () -> Unit,
    onChangePasswordClicked: () -> Unit,
    onManagePasskeysClicked: () -> Unit,
    uiState: SettingsUiState,
) {
    Column(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.dimen_standard)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
    ) {
        Text(stringResource(R.string.security))

        if (uiState.userHasPasskeys) {
            PasskeysManagementTab(
                onManageClicked = onManagePasskeysClicked,
                uiState = uiState,
            )
        } else {
            CreatePasskeyTab(
                onLearnMoreClicked = onLearnMoreClicked,
                onCreatePasskeyClicked = onCreatePasskeyClicked,
                isButtonEnabled = !uiState.isLoading,
            )
        }

        PasswordManagementTab(
            onChangePasswordClicked = onChangePasswordClicked,
            lastPasswordChange = uiState.passwordChanged,
            isButtonEnabled = !uiState.isLoading,
        )
    }
}

/**
 * Composable for the Passkeys Management Tab UI Element
 *
 * @param onManageClicked onClick lambda for manage tab, navigates to the list of passkeys screen
 * @param uiState [SettingsUiState] Holding the data to update the Composable
 * */
@Composable
fun PasskeysManagementTab(
    onManageClicked: () -> Unit,
    uiState: SettingsUiState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.size_standard)))
            .background(grayBackground)
            .padding(dimensionResource(R.dimen.dimen_standard)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_passkey),
            contentDescription = stringResource(R.string.icon_passkeys),
        )

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
        ) {
            Text(
                text = stringResource(R.string.passkeys),
                style = MaterialTheme.typography.displayMedium,
            )

            Text(
                text = "${uiState.passkeysList.size} passkey" + if (uiState.passkeysList.size == 1) {
                    ""
                } else {
                    "s"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(
            onClick = onManageClicked,
            enabled = !uiState.isLoading,
        ) {
            Text(
                stringResource(R.string.manage),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/**
 * Composable for the Create Passkey UI element
 *
 * @param onLearnMoreClicked onclick lambda that redirect to learn more about passkeys on youtube
 * @param onCreatePasskeyClicked onclick lambda to navigate to the [CreatePasskeyScreen]
 * @param isButtonEnabled Boolean to disable buttons if the screen is loading
 * */
@Composable
fun CreatePasskeyTab(
    onLearnMoreClicked: () -> Unit,
    onCreatePasskeyClicked: () -> Unit,
    isButtonEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.size_standard)))
            .background(grayBackground)
            .padding(dimensionResource(R.dimen.padding_large)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        ) {
            Column(
                modifier = Modifier.weight(0.6F),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
            ) {
                Text(
                    text = stringResource(R.string.sign_in_faster_next_time),
                    style = MaterialTheme.typography.bodyLarge,
                )

                ShrineClickableText(
                    text = stringResource(R.string.create_passkey_text),
                    clickableText = stringResource(R.string.how_passkeys_work),
                    onTextClick = onLearnMoreClicked,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            Image(
                modifier = Modifier.weight(0.4F),
                painter = painterResource(R.drawable.ic_passkeys_info),
                contentDescription = "",
            )
        }

        ShrineButton(
            onClick = onCreatePasskeyClicked,
            buttonText = stringResource(R.string.create_passkey),
            isButtonEnabled = isButtonEnabled,
        )
    }
}

/**
 * Composable for the Password Management Tab UI Element
 *
 * @param onChangePasswordClicked onclick lambda to trigger password change flow
 * @param lastPasswordChange Date when the last time password was changed
 * @param isButtonEnabled
 * @param isButtonEnabled Boolean to disable buttons if the screen is loading
 * */
@Composable
fun PasswordManagementTab(
    onChangePasswordClicked: () -> Unit,
    lastPasswordChange: String,
    isButtonEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.size_standard)))
            .background(grayBackground)
            .padding(dimensionResource(R.dimen.dimen_standard)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.clip_path_group),
            contentDescription = "",
        )

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_extra_small)),
        ) {
            Text(
                text = stringResource(R.string.password),
                style = MaterialTheme.typography.displayMedium,
            )

            Text(
                text = stringResource(R.string.last_changed, lastPasswordChange),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        TextButton(
            onClick = onChangePasswordClicked,
            enabled = isButtonEnabled,
        ) {
            Text(
                text = stringResource(R.string.change),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/**
 * Preview of the stateless composable of the Settings Screen
 * */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingPreview() {
    ShrineTheme {
        SettingsScreen(
            onCreatePasskeyClicked = { },
            onChangePasswordClicked = { },
            onLearnMoreClicked = { },
            onManagePasskeysClicked = { },
            onBackClicked = { },
            uiState = SettingsUiState(userHasPasskeys = false),
        )
    }
}
