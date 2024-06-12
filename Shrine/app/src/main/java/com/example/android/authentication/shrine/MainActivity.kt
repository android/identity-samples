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
package com.example.android.authentication.shrine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.android.authentication.shrine.ui.ShrineNavigation
import com.example.android.authentication.shrine.ui.navigation.ShrineAppDestinations
import com.example.android.authentication.shrine.ui.theme.ShrineTheme
import com.example.android.authentication.shrine.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The main activity and the entry point of the Shrine app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        var startDestination = ShrineAppDestinations.AuthRoute.name
        CoroutineScope(Dispatchers.Main).launch {
            startDestination = if (splashViewModel.isSignedInThroughPassword()) {
                if (splashViewModel.isSignedInThroughPasskeys()) {
                    ShrineAppDestinations.MainMenuRoute.name
                } else {
                    ShrineAppDestinations.CreatePasskeyRoute.name
                }
            } else {
                ShrineAppDestinations.AuthRoute.name
            }
        }

        setContent {
            // Setting theme for the App
            ShrineTheme {
                ShrineNavigation(startDestination)
            }
        }
    }
}
