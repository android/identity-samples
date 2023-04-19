/*
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.credentialmanager.sample.repository

import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.credentialmanager.sample.api.ApiException
import com.google.credentialmanager.sample.api.ApiResult
import com.google.credentialmanager.sample.api.AuthApi
import com.google.credentialmanager.sample.api.Credential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Works with the API, the local data store
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope
) {

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
     * Sends the username to the server. If it succeeds, the sign-in state will proceed to
     * [SignInState.SigningIn].
     */
    suspend fun login(username: String, password: String): Boolean {
        return when (val result = api.username(username)) {
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
     * Signs in with a password. This should be called only when the sign-in state is
     * [SignInState.SigningIn]. If it succeeds, the sign-in state will proceed to
     * [SignInState.SignedIn].
     */
    private suspend fun setSessionWithPassword(password: String): Boolean {
        val username = dataStore.read(USERNAME)
        val sessionId = dataStore.read(SESSION_ID)
        if (!username.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
            try {
                when (val result = api.password(sessionId, password)) {
                    ApiResult.SignedOutFromServer -> {
                        signOut()
                        return false
                    }
                    is ApiResult.Success<*> -> {
                        if (result.sessionId != null) {
                            dataStore.edit { prefs ->
                                prefs[SESSION_ID] = result.sessionId
                            }
                        }
                        return true
                    }
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Invalid login credentials", e)

                // start login over again
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

    private fun List<Credential>.toStringSet(): Set<String> {
        return mapIndexed { index, credential ->
            "$index;${credential.id};${credential.publicKey}"
        }.toSet()
    }

    /**
     * Clears all the sign-in information. The sign-in state will proceed to
     * [SignInState.SignedOut].
     */
    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(USERNAME)
            prefs.remove(SESSION_ID)
            prefs.remove(IS_SIGNED_IN_THROUGH_PASSKEYS)
        }
    }

    /**
     * Starts to register a new credential to the server. This should be called only when the
     * sign-in state is [SignInState.SignedIn].
     */
    suspend fun registerRequest(): JSONObject? {
        try {
            val sessionId = dataStore.read(SESSION_ID)
            if (!sessionId.isNullOrEmpty()) {
                when (val apiResult = api.registerRequest(sessionId)) {
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
     * a call to [registerRequest] and a local  API for public key generation.
     */
    suspend fun registerResponse(credentialResponse: CreatePublicKeyCredentialResponse): Boolean {
        try {
            val registrationResponseJson = credentialResponse.registrationResponseJson
            val obj = JSONObject(registrationResponseJson)
            val response = obj.getJSONObject("response")
            val sessionId = dataStore.read(SESSION_ID)!!
            val credentialId = obj.getString("rawId")
            when (val result = api.registerResponse(sessionId, response, credentialId)) {
                ApiResult.SignedOutFromServer -> {
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
            Log.e(TAG, "Cannot call registerResponse", e)
        }
        return false
    }

    /**
     * Starts to sign in with a credential. This should only be called when the sign-in state
     * is [SignInState.SigningIn].
     */
    suspend fun signinRequest(): JSONObject? {
        when (val apiResult = api.signinRequest()) {
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
     * Finishes to signing in with a  credential. This should only be called after a call to
     * [signinRequest] and a local API for key assertion.
     */
    suspend fun signinResponse(credentialResponse: GetCredentialResponse): Boolean {
        try {
            val signinResponse =
                credentialResponse.credential.data.getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON")
            signinResponse?.let {
                val obj = JSONObject(it)
                val response = obj.getJSONObject("response")
                val sessionId = dataStore.read(SESSION_ID)!!
                val credentialId = obj.getString("rawId")
                when (val result = api.signinResponse(sessionId, response, credentialId)) {
                    ApiResult.SignedOutFromServer -> {
                        signOut()
                        return false
                    }
                    is ApiResult.Success -> {
                        dataStore.edit { prefs ->
                            result.sessionId?.let { prefs[SESSION_ID] = it }
                        }
                    }
                }
            }
            return true
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call registerResponse", e)
        }
        return false
    }

    suspend fun isSignedIn(): Boolean {
        val sessionId = dataStore.read(SESSION_ID)
        return when {
            sessionId.isNullOrBlank() -> false
            else -> true
        }
    }

    suspend fun isSignedInThroughPasskeys(): Boolean {
        val isSignedInThroughPasskeys = dataStore.read(IS_SIGNED_IN_THROUGH_PASSKEYS)
        isSignedInThroughPasskeys?.let {
            return it
        }
        return false
    }

    suspend fun setSignedInState(flag: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_SIGNED_IN_THROUGH_PASSKEYS] = flag
        }
    }
}

