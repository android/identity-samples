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

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.authentication.shrine.api.*
import kotlinx.coroutines.delay


/**
 * Defines the types of credentials supported for sign-in.
 */
enum class CredentialType {
    /** Represents a Passkey credential. */
    PASSKEY,

    /** Represents a Password credential. */
    PASSWORD,

    /** Represents a Sign-In With Google (SIWG) credential. */
    SIWG,
}

/**
 * Handles authentication operations using the Android Credential Manager API.
 *
 * This class interacts with an [AuthenticationServer] to facilitate sign-in processes
 * using Passkeys, Passwords, and Sign-In With Google credentials.
 *
 * @param context The Android [Context] used to create the [CredentialManager].
 */
class CredentialManagerAuthenticator(context: Context) {
    private val authenticationServer = AuthenticationServer()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Initiates a sign-in flow using the Android Credential Manager.
     *
     * This method attempts to retrieve credentials based on the provided [types].
     * It first fetches public key server parameters (if relevant for Passkeys),
     * then calls [CredentialManager.getCredential]. It includes a workaround for
     * a known issue where the `getCredential` call might still be finishing
     * after activity resumption, using a progressive delay strategy.
     *
     * @param activity The [Context] (preferably a [ComponentActivity]) used for the `getCredential` call.
     * @param types A list of [CredentialType]s to request. Defaults to all available types if not specified.
     * @return `true` if the credential response was successfully processed and authentication occurred, `false` otherwise.
     */
    suspend fun signInWithCredentialManager(
        activity: Context,
        types: List<CredentialType> = CredentialType.entries,
    ): Boolean {
        val getCredentialRequest = createGetCredentialRequest(types)
        Log.e("john", getCredentialRequest.credentialOptions.toString())
        // confirmed here
        val getCredentialResponse = credentialManager.getCredential(activity, getCredentialRequest)
        // never gets to here.

        Log.e("john", "here4")
        delay(1000L * 3)


        val result = authenticate(getCredentialResponse)
        Log.e("john", "here5")

        return result
    }

    /**signInWithPasskeysRequest
     * Creates a [GetCredentialRequest] based on the desired credential types and a public key request JSON.
     *
     * @param types A list of [CredentialType]s to include in the request. Defaults to all available types.
     * @param requestJSON A JSON string containing the public key credential request options (for Passkeys).
     * @return A configured [GetCredentialRequest] ready to be used with [CredentialManager.getCredential].
     */
    private suspend fun createGetCredentialRequest(
        types: List<CredentialType> = CredentialType.entries
    ): GetCredentialRequest {
        val userCredentialOptions = types.map {
            when (it) {
                CredentialType.PASSKEY -> GetPublicKeyCredentialOption(
                    authenticationServer.getPublicKeyRequestOptions()
                )
                CredentialType.PASSWORD -> GetPasswordOption()
                CredentialType.SIWG -> authenticationServer.createGetGoogleIdOption()
            }
        }
        // confirmed here
        return GetCredentialRequest(userCredentialOptions)
    }


    /**
     * Processes the [GetCredentialResponse] received from [CredentialManager.getCredential].
     *
     * It dispatches the credential to the appropriate authentication method on the [AuthenticationServer]
     * based on the credential type.
     *
     * @param getCredentialResponse The response object from the Credential Manager.
     * @param cookie A cookie string required for Passkey authentication.
     * @return `true` if the credential was successfully processed and authenticated, `false` otherwise.
     */
    private suspend fun authenticate(
        getCredentialResponse: GetCredentialResponse,
    ): Boolean {
        Log.e("john", "here6=")
        when (val credential = getCredentialResponse.credential) {
            is PublicKeyCredential -> {
                return authenticationServer.loginWithPasskey(credential)
            }

            is PasswordCredential -> {
                return authenticationServer.loginWithPassword(credential)
            }

            is CustomCredential -> {
                // This section is only shown for educational purposes, as google sign in is
                // built-in to credential manager with no action needed from the developer.
                if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Log.e(TAG, ERROR_UNRECOGNIZED_CUSTOM.format(credential.type))
                    return false
                }
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return authenticationServer.loginWithGoogleToken(googleIdTokenCredential.idToken)
            }

            else -> {
                Log.w(TAG, WARNING_UNKNOWN_TYPE.format(credential.javaClass.simpleName))
                return false
            }
        }
    }

    /**
     * Registers an authenticated Google ID token with the application's credential repository
     * on the authentication server. This is typically used for integrating legacy Google Sign-In
     * flows with the application's backend authentication.
     *
     * @param token The Google ID token to register.
     */
    suspend fun registerAuthenticatedGoogleToken(token: String) {
        authenticationServer.loginWithGoogleToken(token)
    }


    /**
     * Performs a sign-out operation on the authentication server.
     */
    fun signOut() {
        authenticationServer.signOut()
    }

    /**
     * Companion object holding constants, primarily for logging tags and error/warning messages.
     */
    companion object {
        private const val TAG = "CredentialManagerAuthenticator"
        private const val ERROR_LEGACY_SIWG_REGISTRY =
            "Signed in, but failed to register legacy Google Sign in account to app credential repository. Error: %s"
        private const val ERROR_UNRECOGNIZED_CUSTOM = "Unrecognized CustomCredential: %s"
        private const val WARNING_UNKNOWN_TYPE = "Unknown type: %s"
    }
}
