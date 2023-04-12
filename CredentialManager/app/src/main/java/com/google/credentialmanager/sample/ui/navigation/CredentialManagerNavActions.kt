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

package com.google.credentialmanager.sample.ui.navigation

import androidx.navigation.NavHostController

object CredManAppDestinations {
    const val HOME_ROUTE = "home"
    const val PASSKEYS_ROUTE = "passkeys"
    const val AUTH_ROUTE = "Auth"
    const val SPLASH_ROUTE = "splash"
}

//This controller will help decide where to navigate
class CredentialManagerNavActions(navController: NavHostController) {
    //Takes user to Home flow.
    val navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit = {
        if (it) {
            navController.navigate(CredManAppDestinations.HOME_ROUTE) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(CredManAppDestinations.PASSKEYS_ROUTE) {
                launchSingleTop = true
            }
        }
    }

    //Takes user to login flow.
    val navigateToLogin: () -> Unit = {
        navController.navigate(CredManAppDestinations.AUTH_ROUTE) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}
