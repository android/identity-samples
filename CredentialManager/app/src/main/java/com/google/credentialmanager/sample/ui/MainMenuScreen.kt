/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.credentialmanager.sample.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.LogoHeading
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme
import com.google.credentialmanager.sample.ui.theme.light_button
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel

@Composable
fun MainMenuRoute(
    onShrineButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onHelpButtonClicked: () -> Unit,
    navigateToLogin: () -> Unit,
    viewModel: HomeViewModel
) {

    val uiState = viewModel.uiState.collectAsState().value
    MainMenuScreen(
        onShrineButtonClicked,
        onSettingsButtonClicked,
        onHelpButtonClicked,
        navigateToLogin,
        viewModel::signOut,
        uiState
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun MainMenuScreen(
    onShrineButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onHelpButtonClicked: () -> Unit,
    navigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    uiState: HomeUiState
) {

    val activity = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
        ) {
            LogoHeading()
        }
        Spacer(modifier = Modifier.padding(30.dp))

        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ShrineButton(
                onClick = onShrineButtonClicked,
            ) { Text(stringResource(R.string.shop)) }

            Spacer(modifier = Modifier.padding(10.dp))

            ShrineButton(
                color = light_button,
                onClick = onSettingsButtonClicked
            ) { Text(stringResource(R.string.settings)) }

            Spacer(modifier = Modifier.padding(10.dp))

            ShrineButton(
                onClick = onHelpButtonClicked,
            ) { Text(stringResource(R.string.help)) }

            Spacer(modifier = Modifier.padding(10.dp))

            ShrineButton(
                color = light_button,
                onClick = {
                onSignOut()
                navigateToLogin()
            }) {
                Text(text = stringResource(R.string.sign_out))
            }
        }

        //Handle UiState values
        when (uiState) {
            is HomeUiState.Empty -> {}
            is HomeUiState.MsgString -> {
                LaunchedEffect(uiState) {
                    Toast.makeText(activity, uiState.msg, Toast.LENGTH_LONG).show()
                }
            }
            else -> {}
        }
    }

    BackHandler(enabled = true) {
        activity.finish()
    }
}

@Preview(showBackground = true)
@Composable
fun PasskeysSignedPreview(){
    CredentialManagerTheme {
        MainMenuScreen(
            onShrineButtonClicked = {},
            onSettingsButtonClicked = {},
            onHelpButtonClicked = {},
            navigateToLogin = {},
            onSignOut = {},
            HomeUiState.Empty
        )
    }
}