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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.ui.AuthenticationScreen
import com.authentication.shrine.ui.CreatePasskeyScreen
import com.authentication.shrine.ui.HelpScreen
import com.authentication.shrine.ui.LearnMoreScreen
import com.authentication.shrine.ui.MainMenuScreen
import com.authentication.shrine.ui.PlaceholderScreen
import com.authentication.shrine.ui.RegisterScreen
import com.authentication.shrine.ui.SettingsScreen
import com.authentication.shrine.ui.ShrineAppScreen

/**
 * The navigation graph for the Shrine app.
 *
 * This graph handles navigation between the authentication and contacts screens.
 *
 * @param modifier The modifier to apply to the NavHost.
 * @param navController The NavHostController for navigation.
 * @param startDestination The route to navigate to when the graph is first created.
 * @param navigateToLogin A lambda that navigates to the login screen.
 * @param navigateToHome A lambda that navigates to the home screen.
 * @param navigateToMainMenu A lambda that navigates to the main menu screen.
 * @param navigateToRegister A lambda that navigates to the register screen.
 */
@Composable
fun ShrineNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ShrineAppDestinations.AuthRoute.name,
    navigateToLogin: () -> Unit,
    navigateToHome: (Boolean) -> Unit,
    navigateToMainMenu: (Boolean) -> Unit,
    navigateToRegister: () -> Unit,
    credentialManagerUtils: CredentialManagerUtils,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        route = ShrineAppDestinations.NavHostRoute.name,
    ) {
        composable(route = ShrineAppDestinations.AuthRoute.name) {
            AuthenticationScreen(
                navigateToHome = navigateToHome,
                navigateToRegister = navigateToRegister,
                viewModel = hiltViewModel(),
                credentialManagerUtils = credentialManagerUtils,
            )
        }

        composable(route = ShrineAppDestinations.CreatePasskeyRoute.name) {
            CreatePasskeyScreen(
                navigateToMainMenu = navigateToMainMenu,
                viewModel = hiltViewModel(),
                onLearnMoreClicked = { navController.navigate(ShrineAppDestinations.LearnMore.name) },
                onNotNowClicked = { navController.navigate(ShrineAppDestinations.MainMenuRoute.name) },
                credentialManagerUtils = credentialManagerUtils,
            )
        }

        composable(route = ShrineAppDestinations.MainMenuRoute.name) {
            MainMenuScreen(
                onShrineButtonClicked = { navController.navigate(ShrineAppDestinations.ShrineApp.name) },
                onSettingsButtonClicked = { navController.navigate(ShrineAppDestinations.Settings.name) },
                onHelpButtonClicked = { navController.navigate(ShrineAppDestinations.Help.name) },
                navigateToLogin = navigateToLogin,
                viewModel = hiltViewModel(),
                credentialManagerUtils = credentialManagerUtils,
            )
        }

        composable(route = ShrineAppDestinations.RegisterRoute.name) {
            RegisterScreen(
                navigateToHome = navigateToHome,
                viewModel = hiltViewModel(),
                credentialManagerUtils = credentialManagerUtils,
            )
        }

        composable(route = ShrineAppDestinations.Help.name) {
            HelpScreen()
        }

        composable(route = ShrineAppDestinations.LearnMore.name) {
            LearnMoreScreen(
                onBackButtonClicked = { navController.popBackStack() },
            )
        }

        composable(route = ShrineAppDestinations.Placeholder.name) {
            PlaceholderScreen()
        }

        composable(route = ShrineAppDestinations.Settings.name) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onCreatePasskeyClicked = {
                    navController.navigate(ShrineAppDestinations.CreatePasskeyRoute.name)
                },
                onChangePasswordClicked = {
                    navController.navigate(ShrineAppDestinations.Placeholder.name)
                },
                onLearnMoreClicked = {
                    navController.navigate(ShrineAppDestinations.LearnMore.name)
                },
                onManagePasskeysClicked = {
                    navController.navigate(ShrineAppDestinations.Placeholder.name)
                },
            )
        }

        composable(route = ShrineAppDestinations.PasskeyManagementTab.name) {

        }

        composable(route = ShrineAppDestinations.ShrineApp.name) {
            ShrineAppScreen()
        }
    }
}
