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
package com.example.android.authentication.myvault.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object MyVaultDestinations {
    const val HOME_ROUTE = "home"
    const val SETTINGS_ROUTE = "settings"
}

/**
 * Class that handles navigation actions for the MyVault app.
 *
 * @param navController The NavHostController used to navigate between destinations.
 */
class MyVaultNavActions(navController: NavHostController) {
    /**
     * Navigates to the home destination.
     */
    val navigateToHome: () -> Unit = {
        navController.navigate(MyVaultDestinations.HOME_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * Navigates to the settings destination.
     */
    val navigateToSettings: () -> Unit = {
        navController.navigate(MyVaultDestinations.SETTINGS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
