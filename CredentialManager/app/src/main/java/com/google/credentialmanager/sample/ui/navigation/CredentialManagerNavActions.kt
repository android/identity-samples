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

import androidx.annotation.StringRes
import androidx.navigation.NavHostController
import com.google.credentialmanager.sample.R

enum class CredManAppDestinations(@StringRes val title: Int) {
    CREATE_PASSKEY_ROUTE(title = R.string.home),
    MAIN_MENU_ROUTE(title = R.string.main_menu),
    AUTH_ROUTE(R.string.auth),
    SPLASH_ROUTE(R.string.splash),
    REGISTER_ROUTE(R.string.register),
    Help(title = R.string.help),
    LearnMore(title = R.string.learn_more),
    MainMenu(title = R.string.main_menu),
    Placeholder(title = R.string.todo),
    Settings(title = R.string.settings),
    ShrineApp(title = R.string.app_name),
}

//This controller will help decide where to navigate
class CredentialManagerNavActions(navController: NavHostController) {
    //Takes user to Home flow.
    val navigateToHome: (isSignInThroughPasskeys: Boolean) -> Unit = {
        if (it) {
            navController.navigate(CredManAppDestinations.CREATE_PASSKEY_ROUTE.name) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(CredManAppDestinations.MAIN_MENU_ROUTE.name) {
                launchSingleTop = true
            }
        }
    }

    //Takes user to login flow.
    val navigateToLogin: () -> Unit = {
        navController.navigate(CredManAppDestinations.AUTH_ROUTE.name) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    //Takes user to register flow.
    val navigateToRegister: () -> Unit = {
        navController.navigate(CredManAppDestinations.REGISTER_ROUTE.name) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    //Takes user to Home flow.
    val navigateToMainMenu: (isSignInThroughPasskeys: Boolean) -> Unit = {
        if (it) {
            navController.navigate(CredManAppDestinations.MAIN_MENU_ROUTE.name) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(CredManAppDestinations.CREATE_PASSKEY_ROUTE.name) {
                launchSingleTop = true
            }
        }
    }
}
