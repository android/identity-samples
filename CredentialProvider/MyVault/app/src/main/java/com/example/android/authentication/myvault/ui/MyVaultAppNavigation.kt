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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Stateful version : Provides the state values to be password to the UI for top-level theming and navigation structure for the MyVault application.
 * This composable manages the system UI appearance, navigation drawer, and the core
 * navigation graph using Jetpack Compose.
 */
@Composable
fun MyVaultAppNavigation() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        MyVaultNavActions(navController)
    }
    val coroutineScope = rememberCoroutineScope()

    MyVaultAppNavigation(drawerState, navController, navigationActions, coroutineScope)
}

/**
 * Stateless version : Provides the top-level theming and navigation structure for the MyVault application.
 * This composable manages the system UI appearance, navigation drawer, and the core
 * navigation graph using Jetpack Compose.
 *
 * @param drawerState Controls the open/closed state of the navigation drawer.
 * @param navController The NavHostController used to manage navigation within the app.
 * @param navigationActions Provides actions for common navigation events (home, settings, etc.).
 * @param coroutineScope A CoroutineScope used to launch coroutines, primarily for drawer interactions.
 */
@Composable
fun MyVaultAppNavigation(
    drawerState: DrawerState,
    navController: NavHostController,
    navigationActions: MyVaultNavActions,
    coroutineScope: CoroutineScope,
) {
    val systemUiController = rememberSystemUiController()
    val barColor = MaterialTheme.colorScheme.surface
    SideEffect {
        systemUiController.setSystemBarsColor(barColor)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute =
        navBackStackEntry?.destination?.route ?: MyVaultDestinations.HOME_ROUTE

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(.7f),
            ) {
                AppDrawer(
                    currentRoute = currentRoute,
                    navigateToHome = navigationActions.navigateToHome,
                    navigateToSettings = navigationActions.navigateToSettings,
                    closeDrawer = { coroutineScope.launch { drawerState.close() } },
                )
            }
        },
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
    ) {
        MyVaultNavGraph(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            navController = navController,
            openDrawer = {
                coroutineScope.launch {
                    drawerState.open()
                }
            },
        )
    }
}
