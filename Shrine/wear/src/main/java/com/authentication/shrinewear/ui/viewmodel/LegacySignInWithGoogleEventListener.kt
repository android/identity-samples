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
package com.authentication.shrinewear.ui.viewmodel

import android.util.Log
import com.authentication.shrinewear.Graph
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener

/**
 * Singleton object that implements the [GoogleSignInEventListener] interface
 * to handle Google Sign-In events within the application.
 *
 * This listener is specifically designed for legacy Google Sign-In flows and
 * is responsible for registering the authenticated Google account's ID token
 * with the application's credential repository.
 */
object LegacySignInWithGoogleEventListener : GoogleSignInEventListener {
    private const val TAG = "LegacySignInWithGoogleEventListener"
    private const val INFO_ACCOUNT_RECEIVED =
        "Legacy Google Account received: %s. Registering to application credential repository"
    private const val ERROR_MISSING_ID_TOKEN =
        "Signed in, but failed to register Legacy Google sign in account to application repository due to missing Google Sign in idToken. " +
                "Verify OAuthClient type is 'web' and that GoogleSignInOptionsBuilder.requestIdToken is passed the correct client id."

    /**
     * Called when a Google Sign-In is successful and a [GoogleSignInAccount] is obtained.
     *
     * This method extracts the ID token from the account and, if present and not empty,
     * registers it with the application's authentication repo. If the ID token is missing,
     * an error log is recorded, indicating a potential misconfiguration in the Google
     * Sign-In setup.
     *
     * @param account The [GoogleSignInAccount] obtained after a successful sign-in.
     */
    override suspend fun onSignedIn(account: GoogleSignInAccount) {
        Log.i(TAG, INFO_ACCOUNT_RECEIVED.format(account.displayName))
        account.idToken?.takeIf { it.isNotEmpty() }?.let { token ->
            Graph.authenticationServer.loginWithFederatedToken(token)
        } ?: run {
            Log.e(TAG, ERROR_MISSING_ID_TOKEN)
        }
    }
}
