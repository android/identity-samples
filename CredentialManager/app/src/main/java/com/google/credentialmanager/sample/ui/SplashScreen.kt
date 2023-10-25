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

package com.google.credentialmanager.sample.ui

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.google.credentialmanager.sample.R
import com.google.credentialmanager.sample.ui.navigation.CredManAppDestinations
import com.google.credentialmanager.sample.ui.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    splashViewModel: SplashViewModel,
    navController: NavController
) {

    val scale = remember {
        androidx.compose.animation.core.Animatable(0f)
    }

    // AnimationEffect
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(
                durationMillis = 800,
                easing = {
                    OvershootInterpolator(4f).getInterpolation(it)
                })
        )
        navController.navigate(
            if (splashViewModel.isSignedIn()) {
                if (splashViewModel.isSignedInThroughPasskeys()) {
                    CredManAppDestinations.PASSKEYS_ROUTE
                } else {
                    CredManAppDestinations.HOME_ROUTE
                }
            } else CredManAppDestinations.AUTH_ROUTE
        )
    }

// Image
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.passkey),
            contentDescription = "Logo",
            modifier = Modifier.scale(scale.value)
        )
    }
}
