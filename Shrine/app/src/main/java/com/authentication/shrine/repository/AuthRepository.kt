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
package com.authentication.shrine.repository

import android.util.Log
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.RestoreCredential
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.authentication.shrine.api.ApiException
import com.authentication.shrine.api.ApiResult
import com.authentication.shrine.api.AuthApi
import com.authentication.shrine.model.PasskeysList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class that handles authentication-related operations.
 *
 * @param authApi The API service for interacting with the server.
 * @param dataStore The data store for storing user data.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: DataStore<Preferences>,
) {

    // Companion object for constants and helper methods
    private companion object {
        const val TAG = "AuthRepository"

        // Keys for SharedPreferences
        val USERNAME = stringPreferencesKey("username")
        val IS_SIGNED_IN_THROUGH_PASSKEYS = booleanPreferencesKey("is_signed_passkeys")
        val SESSION_ID = stringPreferencesKey("session_id")

        suspend fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>): T? {
            return data.map { it[key] }.first()
        }
    }

    /**
     * Sends the username to the server.
     *
     * @param username The username to send.
     * @param password The password to send.
     * @return True if the login was successful, false otherwise.
     */
    suspend fun login(username: String, password: String): Boolean {
        return when (val result = authApi.setUsername(username)) {
            ApiResult.SignedOutFromServer -> {
                signOut()
                false
            }

            is ApiResult.Success<*> -> {
                dataStore.edit { prefs ->
                    prefs[USERNAME] = username
                    prefs[SESSION_ID] = result.sessionId!!
                }
                setSessionWithPassword(password)
                true
            }
        }
    }

    /**
     * Signs in with a password.
     *
     * @param password The password to use.
     * @return True if the sign-in was successful, false otherwise.
     */
    private suspend fun setSessionWithPassword(password: String): Boolean {
        val username = dataStore.read(USERNAME)
        val sessionId = dataStore.read(SESSION_ID)
        if (!username.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
            try {
                return when (val result = authApi.setPassword(sessionId, password)) {
                    ApiResult.SignedOutFromServer -> {
                        signOut()
                        false
                    }

                    is ApiResult.Success<*> -> {
                        if (result.sessionId != null) {
                            dataStore.edit { prefs ->
                                prefs[SESSION_ID] = result.sessionId
                            }
                        }
                        true
                    }
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Invalid login credentials", e)

                // Remove previously stored credentials and start login over again
                dataStore.edit { prefs ->
                    prefs.remove(USERNAME)
                    prefs.remove(SESSION_ID)
                }
            }
        } else {
            Log.e(TAG, "Please check if username and session id is present in your datastore")
        }
        return false
    }

    /**
     * Clears all the sign-in information.
     */
    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(USERNAME)
            prefs.remove(SESSION_ID)
            prefs.remove(IS_SIGNED_IN_THROUGH_PASSKEYS)
        }
    }

    /**
     * Starts to register a passkey creation request to the server.
     *
     * @return The public key credential request options, or null if there was an error.
     */
    suspend fun registerPasskeyCreationRequest(): JSONObject? {
        try {
            val sessionId = dataStore.read(SESSION_ID)
            if (!sessionId.isNullOrEmpty()) {
                when (val apiResult = authApi.registerPasskeyCreationRequest(sessionId)) {
                    ApiResult.SignedOutFromServer -> signOut()
                    is ApiResult.Success -> {
                        if (apiResult.sessionId != null) {
                            dataStore.edit { prefs ->
                                prefs[SESSION_ID] = apiResult.sessionId
                            }
                        }
                        return apiResult.data
                    }
                }
            } else {
                Log.e(TAG, "Please check if session id is present")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot call registerRequest", e)
        }
        return null
    }

    /**
     * Finishes registering a new credential to the server. This should only be called after
     * a call to [registerPasskeyCreationRequest] and a local API for public key generation.
     *
     * @param credentialResponse The credential response.
     * @return True if the registration was successful, false otherwise.
     */
    suspend fun registerPasskeyCreationResponse(
        credentialResponse: CreateCredentialResponse,
    ): Boolean {
        try {
            val registrationResponseJson = when (credentialResponse) {
                is CreatePublicKeyCredentialResponse -> {
                    JSONObject(credentialResponse.registrationResponseJson)
                }

                is CreateRestoreCredentialResponse -> {
                    JSONObject(credentialResponse.responseJson)
                }

                else -> {
                    return false
                }
            }

            val result = authApi.registerPasskeyCreationResponse(
                sessionId = dataStore.read(SESSION_ID)!!,
                response = registrationResponseJson.getJSONObject("response"),
                credentialId = registrationResponseJson.getString("rawId"),
            )

            when (result) {
                is ApiResult.SignedOutFromServer -> {
                    signOut()
                    return false
                }

                is ApiResult.Success -> {
                    dataStore.edit { prefs ->
                        result.sessionId?.let { prefs[SESSION_ID] = it }
                    }
                }
            }
            return true
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call registerPasskeyCreationResponse", e)
        }
        return false
    }

    /**
     * Starts to sign in with a credential.
     *
     * @return The public key credential request options, or null if there was an error.
     */
    suspend fun signInWithPasskeysRequest(): JSONObject? {
        when (val apiResult = authApi.signInWithPasskeysRequest()) {
            ApiResult.SignedOutFromServer -> signOut()
            is ApiResult.Success -> {
                dataStore.edit { prefs ->
                    apiResult.sessionId?.let { prefs[SESSION_ID] = it }
                }
                return apiResult.data
            }
        }
        return null
    }

    /**
     * Finishes to signing in with a credential. This should only be called after a call to
     * [signInWithPasskeysRequest] and a local API for key assertion.
     *
     * @param credentialResponse The credential response.
     * @return True if the sign-in was successful, false otherwise.
     */
    suspend fun signInWithPasskeysResponse(credentialResponse: GetCredentialResponse): Boolean {
        try {
            val signInResponse =
                credentialResponse.credential.data.getString(
                    if (credentialResponse.credential.type == RestoreCredential.TYPE_RESTORE_CREDENTIAL) {
                        "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE"
                    } else {
                        "androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON"
                    },
                )
            if (signInResponse != null) {
                val signInResponseJSON = JSONObject(signInResponse)
                val response = signInResponseJSON.getJSONObject("response")
                val sessionId = dataStore.read(SESSION_ID)!!
                val credentialId = signInResponseJSON.getString("rawId")
                return when (val result = authApi.signInWithPasskeysResponse(sessionId, response, credentialId)) {
                    is ApiResult.SignedOutFromServer -> {
                        signOut()
                        false
                    }

                    is ApiResult.Success -> {
                        dataStore.edit { prefs ->
                            result.sessionId?.let { prefs[SESSION_ID] = it }
                        }
                        true
                    }
                }
            } else if (credentialResponse.credential.type == PasswordCredential.TYPE_PASSWORD_CREDENTIAL) {
                val email =
                    credentialResponse.credential.data.getString("androidx.credentials.BUNDLE_KEY_ID")
                val password =
                    credentialResponse.credential.data.getString("androidx.credentials.BUNDLE_KEY_PASSWORD")
                if (email != null && password != null) {
                    return login(email, password)
                } else {
                    Log.e(TAG, "Cannot call registerResponse")
                }
            } else {
                Log.e(TAG, "Cannot call registerResponse")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call registerResponse")
        }
        return false
    }

    /**
     * Checks if the user is signed in.
     *
     * @return True if the user is signed in, false otherwise.
     */
    suspend fun isSignedInThroughPassword(): Boolean {
        val sessionId = dataStore.read(SESSION_ID)
        return when {
            sessionId.isNullOrBlank() -> false
            else -> true
        }
    }

    /**
     * Checks if the user is signed in through passkeys.
     *
     * @return True if the user is signed in through passkeys, false otherwise.
     */
    suspend fun isSignedInThroughPasskeys(): Boolean {
        val isSignedInThroughPasskeys = dataStore.read(IS_SIGNED_IN_THROUGH_PASSKEYS)
        isSignedInThroughPasskeys?.let {
            return it
        }
        return false
    }

    /**
     * Sets the sign-in state.
     *
     * @param flag True if the user is signed in through passkeys, false otherwise.
     */
    suspend fun setSignedInState(flag: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_SIGNED_IN_THROUGH_PASSKEYS] = flag
        }
    }

    suspend fun clearSessionIdFromDataStore() {
        dataStore.edit { prefs ->
            prefs.remove(SESSION_ID)
        }
    }

    suspend fun getUsername(): String {
        return dataStore.read(USERNAME).orEmpty()
    }

    suspend fun getListOfPasskeys(): PasskeysList? {
        val sessionId = dataStore.read(SESSION_ID)
        if (!sessionId.isNullOrBlank()) {
            when (val apiResult = authApi.getKeys(sessionId)) {
                is ApiResult.SignedOutFromServer -> {
                    signOut()
                }

                is ApiResult.Success -> {
                    dataStore.edit { prefs ->
                        apiResult.sessionId?.let { prefs[SESSION_ID] = it }
                    }
                    return apiResult.data
                }
            }
        }
        return null
    }
}
