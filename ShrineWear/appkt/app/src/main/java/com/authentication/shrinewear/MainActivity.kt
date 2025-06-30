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
package com.authentication.shrinewear

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.MaterialTheme
import com.authentication.shrinewear.ui.ShrineApp

private const val TAG = "MainActivity"

/**
 * The main activity for the Shrine Wear OS application.
 *
 * This activity serves as the entry point of the application, initializing the Compose UI
 * and performing a diagnostic check for the Credential Manager API availability.
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is first created.
     *
     * This method performs the following:
     * - Logs the Android SDK version and whether the Credential Manager service is available.
     * - Calls the superclass's `onCreate` method.
     * - Sets the Compose content of the activity, applying [MaterialTheme] and
     * displaying the [ShrineApp] composable.
     *
     * @param savedInstanceState A [Bundle] containing the activity's previously saved state,
     * or null if the activity is being created for the first time.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(
            TAG,
            "Verifying Credential Manager availability.  Build.VERSION.SDK_INT: ${Build.VERSION.SDK_INT}, has credential manager:" +
                "${getSystemService("credential") != null}",
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShrineApp()
            }
        }
    }
}
