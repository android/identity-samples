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
package com.authentication.shrine

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.authentication.shrine.ui.ShrineNavigation
import com.authentication.shrine.ui.theme.ShrineTheme
import com.authentication.shrine.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The main activity and the entry point of the Shrine app.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var credentialManagerUtils: CredentialManagerUtils

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the device is a TV and set soft input mode accordingly
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        installSplashScreen().setKeepOnScreenCondition {
            splashViewModel.uiState.value.isLoading
        }

        setContent {
            // Setting theme for the App
            ShrineTheme {
                ShrineNavigation(
                    startDestination = splashViewModel.uiState.collectAsState().value.nextScreen,
                    credentialManagerUtils = credentialManagerUtils,
                )
            }
        }
    }
}
