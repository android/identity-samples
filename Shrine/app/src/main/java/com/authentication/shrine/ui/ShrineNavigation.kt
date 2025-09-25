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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.ui.navigation.ShrineNavActions
import com.authentication.shrine.ui.navigation.ShrineNavGraph
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Composable function responsible for setting App theme and navigation.
 */
@Composable
fun ShrineNavigation(
    startDestination: String,
    credentialManagerUtils: CredentialManagerUtils,
) {
    val systemUiController = rememberSystemUiController()
    val darkIcons = isSystemInDarkTheme()

    SideEffect {
        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = darkIcons)
    }

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        ShrineNavActions(navController)
    }

    ShrineNavGraph(
        navController = navController,
        startDestination = startDestination,
        navigateToLogin = navigationActions.navigateToLogin,
        navigateToHome = navigationActions.navigateToHome,
        navigateToMainMenu = navigationActions.navigateToMainMenu,
        navigateToRegister = navigationActions.navigateToRegister,
        credentialManagerUtils = credentialManagerUtils,
    )
}
