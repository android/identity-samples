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
package com.androidauth.shrineWear.ui

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

/**
 * Object containing constant definitions for all navigation routes within the Shrine Wear OS application.
 * These constants are used to define the destinations in the navigation graph.
 */
object ShrineDestinations {
    const val HOME_ROUTE = "home"
    const val LEGACY_LOGIN_ROUTE = "legacy_login"
    const val OAUTH_ROUTE = "oauth_login"
    const val LEGACY_SIWG_ROUTE = "legacy_siwg"
    const val SIGN_OUT_ROUTE = "sign_out"
}

/**
 * A class that encapsulates navigation actions for the Shrine Wear OS application.
 *
 * This class provides methods to navigate to various destinations within the [NavHost],
 * applying common navigation options like popping the back stack and launching as a single top.
 *
 * @param navController The [NavHostController] used to perform navigation operations.
 */
class ShrineNavActions(navController: NavHostController) {
    /**
     * Private extension function on [NavOptionsBuilder] to apply default navigation options.
     *
     * These options include:
     * - `popUpTo(0)`: Pops all destinations up to the start destination of the graph (which remains on the stack).
     * - `launchSingleTop = true`: Ensures that if the destination is already at the top of the stack,
     * a new instance is not created.
     */
    private fun NavOptionsBuilder.defaultNavOptions() {
        popUpTo(0)
        launchSingleTop = true
    }

    val navigateToHome: () -> Unit = {
        navController.navigate(ShrineDestinations.HOME_ROUTE) { defaultNavOptions() }
    }
    val navigateToLegacyLogin: () -> Unit = {
        navController.navigate(ShrineDestinations.LEGACY_LOGIN_ROUTE) { defaultNavOptions() }
    }
    val navigateToOAuth: () -> Unit = {
        navController.navigate(ShrineDestinations.OAUTH_ROUTE) { defaultNavOptions() }
    }
    val navigateToLegacySignInWithGoogle: () -> Unit = {
        navController.navigate(ShrineDestinations.LEGACY_SIWG_ROUTE) { defaultNavOptions() }
    }
    val navigateToSignOut: () -> Unit = {
        navController.navigate(ShrineDestinations.SIGN_OUT_ROUTE) { defaultNavOptions() }
    }
}
