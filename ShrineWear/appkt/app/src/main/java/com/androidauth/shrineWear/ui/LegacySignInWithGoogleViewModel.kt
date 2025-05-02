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
package com.androidauth.shrineWear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.androidauth.shrineWear.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInViewModel

/**
 * A [ViewModelProvider.Factory] for creating instances of [GoogleSignInViewModel]
 * specifically configured for legacy Google Sign-In.
 *
 * This factory ensures that the [GoogleSignInViewModel] is created with the necessary
 * [GoogleSignInClient] initialized with specific Google Sign-In options, including
 * requesting email, server auth code, and ID token using the client ID defined
 * in the [BuildConfig]. It also provides the [LegacySignInWithGoogleEventListener]
 * to handle sign-in events.
 */
val LegacySignInWithGoogleViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = this[APPLICATION_KEY]!!

        val gsiOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(BuildConfig.CLIENT_ID)
            .requestIdToken(BuildConfig.CLIENT_ID)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(application, gsiOptions)

        GoogleSignInViewModel(googleSignInClient, LegacySignInWithGoogleEventListener)
    }
}
