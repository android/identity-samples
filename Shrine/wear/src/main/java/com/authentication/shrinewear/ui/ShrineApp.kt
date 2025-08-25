/*
 * Copyright 2025 The Android Open Source Project
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
package com.authentication.shrinewear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.authentication.shrinewear.ui.navigation.ShrineNavActions
import com.authentication.shrinewear.ui.navigation.ShrineNavGraph

/**
 * The main entry point composable for the Shrine Wear OS application.
 *
 * This composable sets up the navigation controller and actions,
 * then displays initial demo instructions before rendering the main navigation graph.
 */
@Composable
fun ShrineApp() {
    val navController = rememberSwipeDismissableNavController()
    val navigationActions = remember(navController) { ShrineNavActions(navController) }

    ShrineNavGraph(navController = navController, navigationActions = navigationActions)
}