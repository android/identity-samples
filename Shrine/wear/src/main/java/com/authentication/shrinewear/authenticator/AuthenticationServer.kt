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

import android.os.Bundle
import android.util.Log
import com.authentication.shrinewear.AuthenticationState
import com.authentication.shrinewear.Graph
import com.authentication.shrinewear.network.AuthNetworkClient
import com.authentication.shrinewear.network.NetworkResult
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Manages all client-side interactions with the authentication backend server.
 *
 * This class serves as the primary interface for authenticating users using various credential types,
 * including Passkeys (WebAuthn), traditional username/password, and Google ID Tokens. It handles
 * sending credential data to the server, managing the session ID received from successful authentication,
 * and parsing server responses into an [NetworkResult] for consumption by higher-level logic.
 *
 * Includes helper methods for JSON request/response processing and error handling.
 */
class AuthenticationServer(private val authNetworkClient: AuthNetworkClient) {
    private var sessionId: String? = null

    companion object {
        /**
         * The tag for logging.
         */
        private const val TAG = "AuthApi"
    }

    /**
     * Retrieves the public key credential request options from the authentication server.
     *
     * This method fetches the necessary challenge and parameters from the backend
     * to initiate a Passkey (WebAuthn) sign-in flow. On successful retrieval,
     * it updates the internal session ID.
     *
     * @return A JSON string containing the public key request options, or an empty string
     * if the server indicates a sign-out state or an error occurs during retrieval.
     */
    internal suspend fun getPublicKeyRequestOptions(): String {
        return when (val publicKeyRequestOptions =
            authNetworkClient.fetchPublicKeyRequestOptions()) {
            is NetworkResult.Success -> {
                publicKeyRequestOptions.sessionId?.let { newSessionId ->
                    sessionId = newSessionId
                }
                publicKeyRequestOptions.data.toString()
            }

            is NetworkResult.SignedOutFromServer -> {
                signOut()
                ""
            }
        }
    }

    /**
     * Attempts to log in a user by verifying a passkey credential with the server.
     *
     * @param passkeyResponseJSON The passkey from the Android Credential Manager containing
     * the signed authentication challenge.
     * @return `true` on successful login and session update, `false` on failure.
     */
    internal suspend fun loginWithPasskey(passkeyResponseJSON: String): Boolean {
        return when (val authorizationResult = authNetworkClient.authorizePasskeyWithServer(
            passkeyResponseJSON, sessionId
        )) {
            is NetworkResult.Success -> {
                authorizationResult.sessionId?.let { newSessionId ->
                    sessionId = newSessionId
                    return true
                }
                Log.e(TAG, "Passkey authorization succeeded but returned no session ID.")
                false
            }

            is NetworkResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Passkey authorization failed on server")
                false
            }
        }
    }

    /**
     * Attempts to log in a user with the provided username and password.
     *
     * This function handles the full server authentication flow. It updates the local
     * session data on success and clears it on failure.
     *
     * @return `true` on successful login, `false` on failure.
     */
    internal suspend fun loginWithPassword(username: String, password: String): Boolean {
        val usernameSessionId =
            when (val result = authNetworkClient.authorizeUsernameWithServer(username)) {
                is NetworkResult.Success -> {
                    result.sessionId
                }

                is NetworkResult.SignedOutFromServer -> {
                    signOut()
                    Log.e(TAG, "Username ${username} not found in server")
                    return false
                }
            }

        if (usernameSessionId == null) {
            signOut()
            Log.e(TAG, "Did not receive a session ID after submitting username.")
            return false
        }

        return when (val result =
            authNetworkClient.authorizePasswordWithServer(usernameSessionId, password)) {
            is NetworkResult.Success -> {
                result.sessionId?.let { passwordSessionId ->
                    sessionId = passwordSessionId
                }
                true
            }

            is NetworkResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Password: ${password} incorrect")
                sessionId = null
                false
            }
        }
    }

    /**
     * Processes a custom credential, works with google id tokens and can be expanded as a router
     * to handle other federated identity credential types.
     *
     * @return {@code true} if the credential was successfully authorized; {@code false} otherwise.
     */
    internal suspend fun loginWithCustomCredential(type: String, data: Bundle): Boolean {
        val federatedToken: String

        if (type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            federatedToken = GoogleIdTokenCredential.createFrom(data).idToken
        } else {
            Log.e(TAG, "Unrecognized custom credential: ${type}")
            return false
        }

        return loginWithFederatedToken(federatedToken)
    }

    /**
     * Logs in with a federated Id Token.
     *
     * This function first performs a network request to retrieve a session ID. Upon successful
     * retrieval, it then sends the provided federated ID token to the backend's verification
     * endpoint, including the obtained session ID in the request headers.
     *
     * @param federatedToken The federated ID token string obtained from Sign-In.
     * @return `true` if both federated options retrieval and token authorization are successful;
     * `false` otherwise. Failures are typically logged within the function.
     */
    internal suspend fun loginWithFederatedToken(federatedToken: String): Boolean {
        val federatedSessionId: String?

        when (val federationOptions = authNetworkClient.fetchFederationOptions()) {
            is NetworkResult.Success -> {
                federatedSessionId = federationOptions.sessionId
                    ?: throw IllegalStateException("Session ID was null in server response")
            }

            is NetworkResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Failed to get federation options from server: $federationOptions")
                return false
            }
        }

        return when (val authorizationResult =
            authNetworkClient.authorizeFederatedTokenWithServer(
                federatedToken,
                federatedSessionId
            )) {
            is NetworkResult.Success -> {
                this.sessionId = authorizationResult.sessionId
                return true
            }

            is NetworkResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Federated Sign in failed.")
                false
            }
        }
    }

    /**
     * Signs out the current user by updating the `signedInState`.
     *
     * This function sets the internal `signedInState` to `false`, which should trigger
     * UI updates or other logic dependent on the user's sign-in status.
     */
    internal fun signOut() {
        Graph.updateAuthenticationState(AuthenticationState.LOGGED_OUT)
        sessionId = null
    }
}
