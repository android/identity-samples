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

package com.google.credentialmanager.sample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ourDarkColorScheme = darkColorScheme(
    primary = Color.Black,
    secondary = Color.White
)

private val ourLightColorScheme = lightColorScheme(
    primary = Color.Yellow,
    secondary = Color.Black
)

//Handling both dark and night themes for app as per device theme.
@Composable
fun CredentialManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val ourColorScheme = if (darkTheme) ourDarkColorScheme else ourLightColorScheme

    MaterialTheme(
        content = content,
        colorScheme = ourColorScheme
    )
}
