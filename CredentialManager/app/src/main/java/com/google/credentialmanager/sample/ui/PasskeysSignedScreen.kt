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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.common.LogoHeading
import com.google.credentialmanager.sample.ui.common.ShrineButton
import com.google.credentialmanager.sample.ui.common.TextHeader
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel

@Composable
fun PasskeysSignedSRoute(
    navigateToLogin: () -> Unit,
    viewModel: HomeViewModel
) {

    val uiState = viewModel.uiState.collectAsState().value
    PasskeysSignedInScreen(
        navigateToLogin,
        viewModel::signOut,
        uiState
    )
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun PasskeysSignedInScreen(
    navigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    uiState: HomeUiState
) {

    val activity = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column {
            LogoHeading()
        }
        Spacer(modifier = Modifier.padding(30.dp))
        ShrineButton(onClick = { /*TODO*/ }) {
            Text(text = ("Go to the App"))
        }
        ShrineButton(onClick = { /*TODO*/ }) {
            Text(stringResource(R.string.settings))
        }
        ShrineButton(onClick = { /*TODO*/ }) {
            Text(stringResource(R.string.help))
        }
        ShrineButton(onClick = {
            onSignOut()
            navigateToLogin()
        }) {
            Text(text = stringResource(R.string.sign_out))
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
