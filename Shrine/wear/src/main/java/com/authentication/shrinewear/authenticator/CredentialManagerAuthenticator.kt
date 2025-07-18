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
package com.authentication.shrinewear.authenticator

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.authentication.shrinewear.BuildConfig
import com.authentication.shrinewear.extensions.awaitState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

/**
 * Handles authentication operations using the Android Credential Manager API.
 *
 * This class interacts with an [AuthenticationServer] to facilitate sign-in processes
 * using Passkeys, Passwords, and Sign-In With Google credentials.
 *
 * @param context The Android [Context] used to create the [CredentialManager].
 * @param authenticationServer The [AuthenticationServer] responsible for handling authentication requests.
 */
class CredentialManagerAuthenticator(
    applicationContext: Context,
    private val authenticationServer: AuthenticationServer
) {
    private val credentialManager: CredentialManager = CredentialManager.create(applicationContext)

    /**
     * Initiates a sign-in flow using the Android Credential Manager.
     *
     * @param activity The [Context] (preferably a [ComponentActivity]) used for the `getCredential` call.
     * @return `true` if credential manager authenticated the user, else `false`.
     */
    internal suspend fun signInWithCredentialManager(activity: Activity): Boolean {
        val getCredentialResponse: GetCredentialResponse =
            credentialManager.getCredential(activity, createGetCredentialRequest())

        // DANGER: Do not call your auth server until the activity has resumed.
        (activity as? LifecycleOwner)?.lifecycle?.awaitState(Lifecycle.State.RESUMED)

        return authenticate(getCredentialResponse.credential)
    }

    /**signInWithPasskeysRequest
     * Creates a [GetCredentialRequest] with standard Wear Credential types.
     *
     * @return A configured [GetCredentialRequest] ready to be used with [CredentialManager.getCredential].
     */
    private suspend fun createGetCredentialRequest(): GetCredentialRequest {
        return GetCredentialRequest(
            credentialOptions = listOf(
                GetPublicKeyCredentialOption(authenticationServer.getPublicKeyRequestOptions()),
                GetPasswordOption(),
                GetGoogleIdOption.Builder().setServerClientId(BuildConfig.GOOGLE_SIGN_IN_SERVER_CLIENT_ID).build(),
            ),
        )
    }

    /**
     * Routes the credential received from `getCredential` to the appropriate authentication
     * type handler on the [AuthenticationServer].
     *
     * @param credential The selected cre
     * @return `true` if the credential was successfully processed and authenticated, else 'false'.
     */
    private suspend fun authenticate(credential: Credential): Boolean {
        when (credential) {
            is PublicKeyCredential -> {
                return authenticationServer.loginWithPasskey(credential.authenticationResponseJson)
            }

            is PasswordCredential -> {
                return authenticationServer.loginWithPassword(
                    credential.id,
                    credential.password
                )
            }

            is CustomCredential -> {
                return authenticationServer.loginWithCustomCredential(
                    credential.type,
                    credential.data
                )
            }

            else -> {
                Log.w(TAG, "Unknown type: ${credential.javaClass.simpleName}")
                return false
            }
        }
    }

    /**
     * Companion object holding constants, primarily for logging tags and error/warning messages.
     */
    companion object {
        private const val TAG = "CredentialManagerAuthenticator"
    }
}
