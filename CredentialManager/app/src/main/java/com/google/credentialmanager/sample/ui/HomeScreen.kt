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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.credentialmanager.sample.ui.viewmodel.HomeUiState
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel

@Composable
fun HomeRoute(navigateToLogin: () -> Unit, viewModel: HomeViewModel) {

    val uiState = viewModel.uiState.collectAsState().value
    HomeScreen(
        navigateToLogin,
        viewModel::signOut,
        uiState
    )
}


@Composable
fun HomeScreen(navigateToLogin: () -> Unit, onSignOut: () -> Unit, uiState: HomeUiState) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                fabShape = RoundedCornerShape(50),
                onSignOut = {
                    onSignOut()
                    navigateToLogin()
                },
                currentEmail = "",
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "${uiState.userName}'s Home Screen",
                fontSize = 20.sp
            )
            Text(
                modifier = Modifier.padding(20.dp),
                text = "This is a sample app to help integrate & test Credential Manager apis.",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun BottomAppBar(
    fabShape: RoundedCornerShape,
    onSignOut: () -> Unit,
    currentEmail: String
) {
    BottomAppBar(
        elevation = 40.dp, cutoutShape = fabShape
    ) {
        Text(
            modifier = Modifier.padding(start = 10.dp), text = currentEmail
        )
        Spacer(Modifier.weight(1f, true))
        IconButton(onClick = {
            onSignOut()
        }) {
            Icon(Filled.Logout, "Sign Out")
        }
    }
}

