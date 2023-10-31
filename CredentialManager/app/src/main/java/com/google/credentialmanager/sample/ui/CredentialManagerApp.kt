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

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.credentialmanager.sample.ui.navigation.CredManAppDestinations
import com.google.credentialmanager.sample.ui.navigation.CredentialManagerNavActions
import com.google.credentialmanager.sample.ui.navigation.CredentialManagerNavGraph
import com.google.credentialmanager.sample.ui.theme.CredentialManagerTheme

//This composable is responsible for setting App theme and navigation
@Composable
fun CredentialManagerApp() {
    CredentialManagerTheme {
        val systemUiController = rememberSystemUiController()
        val darkIcons = MaterialTheme.colors.isLight

        SideEffect {
            systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = darkIcons)
        }

        val navController = rememberNavController()
        val navigationActions = remember(navController) {
            CredentialManagerNavActions(navController)
        }

        CredentialManagerNavGraph(
            navController = navController,
            startDestination = CredManAppDestinations.SPLASH_ROUTE,
            navigateToLogin = navigationActions.navigateToLogin,
            navigateToHome = navigationActions.navigateToHome,
            navigateToRegister = navigationActions.navigateToRegister
        )
    }
}

