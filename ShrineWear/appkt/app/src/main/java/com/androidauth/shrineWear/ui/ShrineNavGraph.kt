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

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen

/**
 * Defines the navigation graph for the Shrine Wear OS application.
 *
 * This composable sets up the various navigation destinations and their associated Composable screens.
 * It uses [NavHost] to manage the navigation state and provides actions for moving between screens.
 *
 * @param navController The [NavHostController] to be used for navigating between destinations.
 * Defaults to a new [NavHostController] created with [rememberNavController].
 * @param startDestination The route for the initial screen to be displayed when the navigation graph is launched.
 * Defaults to [ShrineDestinations.HOME_ROUTE].
 * @param navigationActions An instance of [ShrineNavActions] providing navigation callbacks for the screens.
 */
@Composable
fun ShrineNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = ShrineDestinations.HOME_ROUTE,
    navigationActions: ShrineNavActions,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        val credentialManagerViewModel = CredentialManagerViewModel()
        composable(ShrineDestinations.HOME_ROUTE) {
            HomeScreen(
                credentialManagerViewModel = credentialManagerViewModel,
                navigateToLegacyLogin = navigationActions.navigateToLegacyLogin,
                navigateToSignOut = navigationActions.navigateToSignOut,
            )
        }
        composable(ShrineDestinations.LEGACY_LOGIN_ROUTE) {
            LegacyLoginScreen(
                navigateToOAuth = navigationActions.navigateToOAuth,
                navigateToLegacySignInWithGoogle = navigationActions.navigateToLegacySignInWithGoogle,
                navigateToHome = navigationActions.navigateToHome,
                isCredentialManagerEnabled = isCredentialManagerAvailable(),
            )
        }
        composable(ShrineDestinations.OAUTH_ROUTE) {
            OAuthScreen(
                oAuthViewModel = OAuthViewModel(LocalContext.current.applicationContext as Application),
                navigateToSignOut = navigationActions.navigateToSignOut,
                navigateToLegacyLogin = navigationActions.navigateToLegacyLogin,
            )
        }
        composable(ShrineDestinations.LEGACY_SIWG_ROUTE) {
            assert(!isCredentialManagerAvailable())

            GoogleSignInScreen(
                onAuthCancelled = { navigationActions.navigateToLegacyLogin() },
                onAuthSucceed = { navigationActions.navigateToSignOut() },
                viewModel = viewModel(factory = LegacySignInWithGoogleViewModelFactory),
            )
        }
        composable(ShrineDestinations.SIGN_OUT_ROUTE) {
            SignOutScreen(navigateToHome = navigationActions.navigateToHome)
        }
    }
}

/**
 * Checks if the Credential Manager API is available on the current Android device.
 *
 * The Credential Manager API is available on Android devices running Vanilla Ice Cream (API 34) or higher.
 *
 * @return `true` if Credential Manager is available, `false` otherwise.
 */
fun isCredentialManagerAvailable(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
}
