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
import com.google.credentialmanager.sample.ui.HomeRoute
import com.google.credentialmanager.sample.ui.SplashScreen
import com.google.credentialmanager.sample.ui.viewmodel.AuthenticationViewModel
import com.google.credentialmanager.sample.ui.viewmodel.HomeViewModel
import com.google.credentialmanager.sample.ui.viewmodel.SplashViewModel

//This Navigation Graph is handling to and fro from Authentication to Contacts screens.
@Composable
fun CredentialManagerNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = CredManAppDestinations.AUTH_ROUTE,
    navigateToLogin: () -> Unit,
    navigateToHome: () -> Unit
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
                viewModel = authViewModel
            )
        }
        composable(CredManAppDestinations.HOME_ROUTE) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            HomeRoute(
                navigateToLogin = navigateToLogin,
                viewModel = homeViewModel
            )
        }
        composable(CredManAppDestinations.SPLASH_ROUTE) {
            val splashViewModel = hiltViewModel<SplashViewModel>()
            SplashScreen(
                splashViewModel = splashViewModel,
                navController
            )
        }
    }
}
