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
package com.authentication.shrine.ui.navigation

import androidx.annotation.StringRes
import androidx.navigation.NavHostController
import com.authentication.shrine.R

/**
 * An enum class representing the different destinations in the Shrine app.
 *
 * @param title The resource ID of the destination title string.
 */
enum class ShrineAppDestinations(@StringRes val title: Int) {
    CreatePasskeyRoute(title = R.string.home),
    MainMenuRoute(title = R.string.main_menu),
    AuthRoute(title = R.string.auth),
    RegisterRoute(title = R.string.register),
    Help(title = R.string.help),
    LearnMore(title = R.string.learn_more),
    Placeholder(title = R.string.todo),
    Settings(title = R.string.settings),
    ShrineApp(title = R.string.app_name),
    NavHostRoute(title = R.string.nav_host_route),
}

/**
 * A class that provides navigation actions for the Shrine app.
 * This controller will help decide where to navigate
 *
 * @param navController The [NavHostController] used for navigation.
 */
class ShrineNavActions(navController: NavHostController) {
    // Takes user to Home flow.
    val navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit = {
        if (it) {
            navController.navigate(ShrineAppDestinations.CreatePasskeyRoute.name) {
                popUpTo(ShrineAppDestinations.NavHostRoute.name)
                launchSingleTop = true
            }
        } else {
            navController.navigate(ShrineAppDestinations.MainMenuRoute.name) {
                popUpTo(ShrineAppDestinations.NavHostRoute.name)
                launchSingleTop = true
            }
        }
    }

    /**
     * Navigates to the login flow.
     */
    val navigateToLogin: () -> Unit = {
        navController.navigate(ShrineAppDestinations.AuthRoute.name) {
            popUpTo(ShrineAppDestinations.NavHostRoute.name) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    /**
     * Navigates to the register flow.
     */
    val navigateToRegister: () -> Unit = {
        navController.navigate(ShrineAppDestinations.RegisterRoute.name) {
            launchSingleTop = true
        }
    }

    /**
     * Navigates to the Main Menu flow.
     */
    val navigateToMainMenu: (isSignedInThroughPasskeys: Boolean) -> Unit = {
        if (it) {
            navController.navigate(ShrineAppDestinations.MainMenuRoute.name) {
                popUpTo(ShrineAppDestinations.NavHostRoute.name)
                launchSingleTop = true
            }
        } else {
            navController.navigate(ShrineAppDestinations.CreatePasskeyRoute.name) {
                popUpTo(ShrineAppDestinations.NavHostRoute.name)
                launchSingleTop = true
            }
        }
    }
}
