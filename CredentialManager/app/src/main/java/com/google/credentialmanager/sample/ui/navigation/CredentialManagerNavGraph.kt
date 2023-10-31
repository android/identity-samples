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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.AuthenticationRoute
import com.google.credentialmanager.sample.ui.HelpScreen
import com.google.credentialmanager.sample.ui.HomeRoute
import com.google.credentialmanager.sample.ui.LearnMoreScreen
import com.google.credentialmanager.sample.ui.PasskeysSignedRoute
import com.google.credentialmanager.sample.ui.RegisterRoute
import com.google.credentialmanager.sample.ui.ShrineAppScreen
import com.google.credentialmanager.sample.ui.SplashScreen
import com.google.credentialmanager.sample.ui.viewmodel.AuthenticationViewModel
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import com.google.credentialmanager.sample.ui.viewmodel.SplashViewModel

enum class ApplicationScreen(@StringRes val title: Int){
    AccountRecovery(title = R.string.account_recovery),
    Help(title = R.string.help),
    Home(title = R.string.home),
    LearnMore(title = R.string.learn_more),
    MainMenu(title = R.string.main_menu),
    OtherOptions(title = R.string.other_options),
    Placeholder(title = R.string.todo),
    Settings(title = R.string.settings),
    ShrineApp(title = R.string.app_name),
    SignInWithPasskey(title = R.string.sign_in),
    SignInWithPassword(title = R.string.sign_in),
    SignOut(title = R.string.sign_out),
    SignUpWithPasskey(title = R.string.sign_up),
    SignUpWithPassword(title = R.string.sign_up),
}

//This Navigation Graph is handling to and fro from Authentication to Contacts screens.
@Composable
fun CredentialManagerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = CredManAppDestinations.AUTH_ROUTE,
    navigateToLogin: () -> Unit,
    navigateToHome: (Boolean) -> Unit,
    navigateToRegister: () -> Unit
) {

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(CredManAppDestinations.AUTH_ROUTE) {
            val authViewModel = hiltViewModel<AuthenticationViewModel>()
            AuthenticationRoute(
                navigateToHome = navigateToHome,
                navigateToRegister = navigateToRegister,
                viewModel = authViewModel
            )
        }

        composable(CredManAppDestinations.HOME_ROUTE) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            HomeRoute(
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel,
                onLearnMoreClicked = { navController.navigate(ApplicationScreen.LearnMore.name) }
            )
        }

        composable(CredManAppDestinations.PASSKEYS_ROUTE) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            PasskeysSignedRoute(
                onSettingsButtonClicked = {navController.navigate(ApplicationScreen.ShrineApp.name)},
                onHelpButtonClicked = { navController.navigate(ApplicationScreen.Help.name) },
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel
            )
        }

        composable(CredManAppDestinations.REGISTER_ROUTE) {
            val authViewModel = hiltViewModel<AuthenticationViewModel>()
            RegisterRoute(
                navigateToHome = navigateToHome,
                viewModel = authViewModel
            )
        }

        composable(CredManAppDestinations.SPLASH_ROUTE) {
            val splashViewModel = hiltViewModel<SplashViewModel>()
            SplashScreen(
                splashViewModel = splashViewModel,
                navController
            )
        }

        composable(route = ApplicationScreen.Help.name) {
            HelpScreen()
        }

        composable(route = ApplicationScreen.LearnMore.name) {
            LearnMoreScreen()
        }

        composable(route = ApplicationScreen.ShrineApp.name) {
            ShrineAppScreen()
        }
    }
}
