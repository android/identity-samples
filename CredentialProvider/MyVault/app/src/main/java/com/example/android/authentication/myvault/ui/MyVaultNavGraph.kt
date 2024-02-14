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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.ui.home.HomeScreen
import com.example.android.authentication.myvault.ui.home.HomeViewModelFactory
import com.example.android.authentication.myvault.ui.settings.SettingsScreen
import com.example.android.authentication.myvault.ui.settings.SettingsViewModelFactory

/**
 * Composable that represents the navigation graph for the MyVault app.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param navController The NavHostController used to navigate between destinations.
 * @param openDrawer A function that opens the drawer.
 * @param startDestination The route of the start destination.
 */
@Composable
fun MyVaultNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
    startDestination: String = MyVaultDestinations.HOME_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxHeight(),
    ) {
        composable(MyVaultDestinations.HOME_ROUTE) {
            HomeScreen(
                homeViewModel = viewModel(
                    factory = HomeViewModelFactory(
                        AppDependencies.credentialsDataSource,
                        AppDependencies.RPIconDataSource,
                    ),
                ),
                openDrawer = openDrawer,
            )
        }
        composable(MyVaultDestinations.SETTINGS_ROUTE) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModelFactory(AppDependencies.database)),
                openDrawer = openDrawer,
            )
        }
    }
}
