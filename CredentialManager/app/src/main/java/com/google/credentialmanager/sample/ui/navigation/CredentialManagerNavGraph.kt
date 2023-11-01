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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.ui.AuthenticationRoute
import com.google.credentialmanager.sample.ui.HelpScreen
import com.google.credentialmanager.sample.ui.HomeRoute
import com.google.credentialmanager.sample.ui.LearnMoreScreen
import com.google.credentialmanager.sample.ui.PasskeysSignedInRoute
import com.google.credentialmanager.sample.ui.PlaceholderScreen
import com.google.credentialmanager.sample.ui.RegisterRoute
import com.google.credentialmanager.sample.ui.SettingsScreen
import com.google.credentialmanager.sample.ui.ShrineAppScreen
import com.google.credentialmanager.sample.ui.SplashScreen
import com.google.credentialmanager.sample.ui.viewmodel.AuthenticationViewModel
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import com.google.credentialmanager.sample.ui.viewmodel.SplashViewModel


//This Navigation Graph is handling to and fro from Authentication to Contacts screens.
@Composable
fun CredentialManagerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = CredManAppDestinations.AUTH_ROUTE.name,
    navigateToLogin: () -> Unit,
    navigateToHome: (Boolean) -> Unit,
    navigateToRegister: () -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = CredManAppDestinations.AUTH_ROUTE.name) {
            val authViewModel = hiltViewModel<AuthenticationViewModel>()
            AuthenticationRoute(
                navigateToHome = navigateToHome,
                navigateToRegister = navigateToRegister,
                viewModel = authViewModel
            )
        }

        composable(route = CredManAppDestinations.HOME_ROUTE.name) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            HomeRoute(
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel,
                onLearnMoreClicked = { navController.navigate(CredManAppDestinations.LearnMore.name) },
                onNotNowClicked = { navController.navigate(CredManAppDestinations.MainMenu.name) }
            )
        }

        composable(route = CredManAppDestinations.PASSKEYS_ROUTE.name) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            PasskeysSignedInRoute(
                onShrineButtonClicked = { navController.navigate(CredManAppDestinations.ShrineApp.name) },
                onSettingsButtonClicked = { navController.navigate(CredManAppDestinations.Settings.name) },
                onHelpButtonClicked = { navController.navigate(CredManAppDestinations.Help.name) },
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel
            )
        }

        composable(route = CredManAppDestinations.REGISTER_ROUTE.name) {
            val authViewModel = hiltViewModel<AuthenticationViewModel>()
            RegisterRoute(
                navigateToHome = navigateToHome,
                viewModel = authViewModel
            )
        }

        composable(route = CredManAppDestinations.SPLASH_ROUTE.name) {
            val splashViewModel = hiltViewModel<SplashViewModel>()
            SplashScreen(
                splashViewModel = splashViewModel,
                navController
            )
        }

        composable(route = CredManAppDestinations.Help.name) {
            HelpScreen()
        }

        composable(route = CredManAppDestinations.LearnMore.name) {
            LearnMoreScreen()
        }

        composable(route = CredManAppDestinations.MainMenu.name) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            PasskeysSignedInRoute(
                onShrineButtonClicked = { navController.navigate(CredManAppDestinations.ShrineApp.name) },
                onSettingsButtonClicked = { navController.navigate(CredManAppDestinations.Settings.name) },
                onHelpButtonClicked = { navController.navigate(CredManAppDestinations.Help.name) },
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel
            )
        }

        composable(route = CredManAppDestinations.Placeholder.name) {
            PlaceholderScreen()
        }

        composable(route = CredManAppDestinations.Settings.name) {
            SettingsScreen(
                onCreatePasskeyClicked = {
                    navController.navigate(CredManAppDestinations.Placeholder.name)
                },
                onChangePasswordClicked = {
                    navController.navigate(CredManAppDestinations.Placeholder.name)
                },
                onHelpClicked = {
                    navController.navigate(CredManAppDestinations.Help.name)
                }
            )
        }

        composable(route = CredManAppDestinations.ShrineApp.name) {
            ShrineAppScreen()
        }
    }
}
