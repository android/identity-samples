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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.android.authentication.myvault.Dimensions
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.data.PasswordItem
import com.example.android.authentication.myvault.data.room.SiteMetaData
import com.example.android.authentication.myvault.data.room.SiteWithCredentials

/**
 * This composable holds the UI logic to show credential details of selected domain/calling app
 * @param site : selected domain/site
 * @param onCancel : method to call on back press
 * @param onPasswordDelete : method to call on selected password credential delete
 * @param onPasskeyDelete : method to call on selected passkey credential delete
 * @param modifier : modifier for the composable
 */
@Composable
fun ShowCredentialsScreen(
    site: SiteWithCredentials,
    onCancel: () -> Unit,
    onPasswordDelete: (PasswordItem) -> Unit,
    onPasskeyDelete: (PasskeyItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    ShowCredentialsScreen(
        snackbarHostState,
        site,
        onCancel,
        onPasswordDelete,
        onPasskeyDelete,
        modifier,
    )
}

/**
 * This composable holds the UI logic to show credential details of selected domain/calling app
 *
 * @param snackbarHostState The state of the SnackbarHost
 * @param site The selected domain/site
 * @param onCancel The callback to be invoked when the user clicks the back button
 * @param onPasswordDelete The callback to be invoked when the user clicks the delete button for a password credential
 * @param onPasskeyDelete The callback to be invoked when the user clicks the delete button for a passkey credential
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ShowCredentialsScreen(
    snackbarHostState: SnackbarHostState,
    site: SiteWithCredentials,
    onCancel: () -> Unit,
    onPasswordDelete: (PasswordItem) -> Unit,
    onPasskeyDelete: (PasskeyItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onCancel()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBarContent(site, onCancel, Modifier)
        },
        modifier = modifier,
    ) { innerPadding ->
        CredentialsEntry(innerPadding, site, onPasskeyDelete, onPasswordDelete, Modifier)
    }
}

/**
 * This composable holds the UI logic to show the top app bar with the site name and a back button.
 *
 * @param site The selected domain/site
 * @param onCancel The callback to be invoked when the user clicks the back button
 * @param modifier The modifier to be applied to the composable
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopAppBarContent(
    site: SiteWithCredentials,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.credentials_for, site.site.url),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
    )
}

/**
 * Renders the credential entries for the selected domain/site.
 *
 * @param innerPadding The padding to apply to the inner content.
 * @param site The SiteWithCredentials object representing the site and its credentials.
 * @param onPasskeyDelete The callback to be invoked when a passkey is deleted.
 * @param onPasswordDelete The callback to be invoked when a password is deleted.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
private fun CredentialsEntry(
    innerPadding: PaddingValues,
    site: SiteWithCredentials,
    onPasskeyDelete: (PasskeyItem) -> Unit,
    onPasswordDelete: (PasswordItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .padding(Dimensions.padding_large)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        items(site.passkeys) {
            PasskeyEntry(
                passkey = it,
                onPasskeyDelete = onPasskeyDelete,
                Modifier,
            )
        }
        items(site.passwords) {
            PasswordEntry(
                password = it,
                onPasswordDelete = onPasswordDelete,
                Modifier,
            )
        }
    }
}

/**
 * Renders the password entry for the selected domain/site.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param password The password item to display.
 * @param onPasswordDelete The callback to be invoked when the user clicks the delete button.
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun PasswordEntry(
    password: PasswordItem,
    onPasswordDelete: (PasswordItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimensions.padding_medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.padding_medium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        ) {
            TextField(
                value = password.username,
                onValueChange = {},
                readOnly = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.outline,
                ),
            )
            TextField(
                value = password.password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                onValueChange = {},
                readOnly = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = {
                    val text = if (passwordVisible) {
                        stringResource(R.string.hide)
                    } else {
                        stringResource(R.string.show)
                    }
                    ClickableText(
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        text = AnnotatedString(text),
                        onClick = { passwordVisible = !passwordVisible },
                    )
                },
            )
            Button(
                modifier = Modifier
                    .padding(horizontal = Dimensions.padding_small)
                    .align(Alignment.End),
                onClick = { onPasswordDelete(password) },
            ) {
                Text(text = stringResource(R.string.delete))
            }
        }
    }
}

/**
 * This composable holds the UI logic to render a single passkey entry.
 *
 * @param passkey The PasskeyItem object representing the passkey
 * @param onPasskeyDelete The callback to be invoked when the user clicks the delete button
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun PasskeyEntry(
    passkey: PasskeyItem,
    onPasskeyDelete: (PasskeyItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimensions.padding_medium),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.padding_medium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.padding_medium),
        ) {
            TextField(
                value = passkey.username,
                onValueChange = {},
                readOnly = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Button(
                modifier = Modifier
                    .padding(horizontal = Dimensions.padding_small)
                    .align(Alignment.End),
                onClick = { onPasskeyDelete(passkey) },
            ) {
                Text(text = stringResource(R.string.delete))
            }
        }
    }
}

/**
 * This composable function provides a preview of the ShowCredentialsScreen composable.
 */
@Preview
@Composable
fun ShowCredentialsScreenPreview() {
    ShowCredentialsScreen(
        snackbarHostState = SnackbarHostState(),
        onCancel = {},
        onPasswordDelete = {},
        onPasskeyDelete = {},
        site = SiteWithCredentials(SiteMetaData(), emptyList(), emptyList()),
        modifier = Modifier,
    )
}
