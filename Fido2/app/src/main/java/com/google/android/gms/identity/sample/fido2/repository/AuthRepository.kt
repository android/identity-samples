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

package com.google.android.gms.identity.sample.fido2.repository

import android.app.PendingIntent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.android.gms.identity.sample.fido2.api.ApiException
import com.google.android.gms.identity.sample.fido2.api.ApiResult
import com.google.android.gms.identity.sample.fido2.api.AuthApi
import com.google.android.gms.identity.sample.fido2.api.Credential
import com.google.android.gms.identity.sample.fido2.toBase64
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Works with the API, the local data store, and FIDO2 API.
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
        val SESSION_ID = stringPreferencesKey("session_id")
        val CREDENTIALS = stringSetPreferencesKey("credentials")
        val LOCAL_CREDENTIAL_ID = stringPreferencesKey("local_credential_id")

        suspend fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>): T? {
            return data.map { it[key] }.first()
        }
    }

    private var fido2ApiClient: Fido2ApiClient? = null

    fun setFido2APiClient(client: Fido2ApiClient?) {
        fido2ApiClient = client
    }

    private val signInStateMutable = MutableSharedFlow<SignInState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** The current [SignInState]. */
    val signInState = signInStateMutable.asSharedFlow()

    /**
     * The list of credentials this user has registered on the server. This is only populated when
     * the sign-in state is [SignInState.SignedIn].
     */
    val credentials =
        dataStore.data.map { it[CREDENTIALS] ?: emptySet() }.map { parseCredentials(it) }

    init {
        scope.launch {
            val username = dataStore.read(USERNAME)
            val sessionId = dataStore.read(SESSION_ID)
            val initialState = when {
                username.isNullOrBlank() -> SignInState.SignedOut
                sessionId.isNullOrBlank() -> SignInState.SigningIn(username)
                else -> SignInState.SignedIn(username)
            }
            signInStateMutable.emit(initialState)
            if (initialState is SignInState.SignedIn) {
                refreshCredentials()
            }
        }
    }

    /**
     * Sends the username to the server. If it succeeds, the sign-in state will proceed to
     * [SignInState.SigningIn].
     */
    suspend fun username(username: String) {
        when (val result = api.username(username)) {
            ApiResult.SignedOutFromServer -> forceSignOut()
            is ApiResult.Success -> {
                dataStore.edit { prefs ->
                    prefs[USERNAME] = username
                    prefs[SESSION_ID] = result.sessionId!!
                }
                signInStateMutable.emit(SignInState.SigningIn(username))
            }
        }
    }

    /**
     * Signs in with a password. This should be called only when the sign-in state is
     * [SignInState.SigningIn]. If it succeeds, the sign-in state will proceed to
     * [SignInState.SignedIn].
     */
    suspend fun password(password: String) {
        val username = dataStore.read(USERNAME)!!
        val sessionId = dataStore.read(SESSION_ID)!!
        try {
            when (val result = api.password(sessionId, password)) {
                ApiResult.SignedOutFromServer -> forceSignOut()
                is ApiResult.Success -> {
                    if (result.sessionId != null) {
                        dataStore.edit { prefs ->
                            prefs[SESSION_ID] = result.sessionId
                        }
                    }
                    signInStateMutable.emit(SignInState.SignedIn(username))
                    refreshCredentials()
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Invalid login credentials", e)

            // start login over again
            dataStore.edit { prefs ->
               prefs.remove(USERNAME)
               prefs.remove(SESSION_ID)
               prefs.remove(CREDENTIALS)
            }

            signInStateMutable.emit(
                SignInState.SignInError(e.message ?: "Invalid login credentials")
            )
        }
    }

    private suspend fun refreshCredentials() {
        val sessionId = dataStore.read(SESSION_ID)!!
        when (val result = api.getKeys(sessionId)) {
            ApiResult.SignedOutFromServer -> forceSignOut()
            is ApiResult.Success -> {
                dataStore.edit { prefs ->
                    result.sessionId?.let { prefs[SESSION_ID] = it }
                    prefs[CREDENTIALS] = result.data.toStringSet()
                }
            }
        }
    }

    private fun List<Credential>.toStringSet(): Set<String> {
        return mapIndexed { index, credential ->
            "$index;${credential.id};${credential.publicKey}"
        }.toSet()
    }

    private fun parseCredentials(set: Set<String>): List<Credential> {
        return set.map { s ->
            val (index, id, publicKey) = s.split(";")
            index to Credential(id, publicKey)
        }.sortedBy { (index, _) -> index }
            .map { (_, credential) -> credential }
    }

    /**
     * Clears the credentials. The sign-in state will proceed to [SignInState.SigningIn].
     */
    suspend fun clearCredentials() {
        val username = dataStore.read(USERNAME)!!
        dataStore.edit { prefs ->
            prefs.remove(CREDENTIALS)
        }
        signInStateMutable.emit(SignInState.SigningIn(username))
    }

    /**
     * Clears all the sign-in information. The sign-in state will proceed to
     * [SignInState.SignedOut].
     */
    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(USERNAME)
            prefs.remove(SESSION_ID)
            prefs.remove(CREDENTIALS)
        }
        signInStateMutable.emit(SignInState.SignedOut)
    }

    private suspend fun forceSignOut() {
        dataStore.edit { prefs ->
            prefs.remove(USERNAME)
            prefs.remove(SESSION_ID)
            prefs.remove(CREDENTIALS)
        }
        signInStateMutable.emit(SignInState.SignInError("Signed out by server"))
    }

    /**
     * Starts to register a new credential to the server. This should be called only when the
     * sign-in state is [SignInState.SignedIn].
     */
    suspend fun registerRequest(): PendingIntent? {
        fido2ApiClient?.let { client ->
            try {
                val sessionId = dataStore.read(SESSION_ID)!!
                when (val apiResult = api.registerRequest(sessionId)) {
                    ApiResult.SignedOutFromServer -> forceSignOut()
                    is ApiResult.Success -> {
                        if (apiResult.sessionId != null) {
                            dataStore.edit { prefs ->
                                prefs[SESSION_ID] = apiResult.sessionId
                            }
                        }
                        val task = client.getRegisterPendingIntent(apiResult.data)
                        return task.await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cannot call registerRequest", e)
            }
        }
        return null
    }

    /**
     * Finishes registering a new credential to the server. This should only be called after
     * a call to [registerRequest] and a local FIDO2 API for public key generation.
     */
    suspend fun registerResponse(credential: PublicKeyCredential) {
        try {
            val sessionId = dataStore.read(SESSION_ID)!!
            val credentialId = credential.rawId!!.toBase64()
            when (val result = api.registerResponse(sessionId, credential)) {
                ApiResult.SignedOutFromServer -> forceSignOut()
                is ApiResult.Success -> {
                    dataStore.edit { prefs ->
                        result.sessionId?.let { prefs[SESSION_ID] = it }
                        prefs[CREDENTIALS] = result.data.toStringSet()
                        prefs[LOCAL_CREDENTIAL_ID] = credentialId
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call registerResponse", e)
        }
    }

    /**
     * Removes a credential registered on the server.
     */
    suspend fun removeKey(credentialId: String) {
        try {
            val sessionId = dataStore.read(SESSION_ID)!!
            when (api.removeKey(sessionId, credentialId)) {
                ApiResult.SignedOutFromServer -> forceSignOut()
                is ApiResult.Success -> refreshCredentials()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call removeKey", e)
        }
    }

    /**
     * Starts to sign in with a FIDO2 credential. This should only be called when the sign-in state
     * is [SignInState.SigningIn].
     */
    suspend fun signinRequest(): PendingIntent? {
        fido2ApiClient?.let { client ->
            val sessionId = dataStore.read(SESSION_ID)!!
            val credentialId = dataStore.read(LOCAL_CREDENTIAL_ID)
            if (credentialId != null) {
                when (val apiResult = api.signinRequest(sessionId, credentialId)) {
                    ApiResult.SignedOutFromServer -> forceSignOut()
                    is ApiResult.Success -> {
                        val task = client.getSignPendingIntent(apiResult.data)
                        return task.await()
                    }
                }
            }
        }
        return null
    }

    /**
     * Finishes to signing in with a FIDO2 credential. This should only be called after a call to
     * [signinRequest] and a local FIDO2 API for key assertion.
     */
    suspend fun signinResponse(credential: PublicKeyCredential) {
        try {
            val username = dataStore.read(USERNAME)!!
            val sessionId = dataStore.read(SESSION_ID)!!
            val credentialId = credential.rawId!!.toBase64()
            when (val result = api.signinResponse(sessionId, credential)) {
                ApiResult.SignedOutFromServer -> forceSignOut()
                is ApiResult.Success -> {
                    dataStore.edit { prefs ->
                        result.sessionId?.let { prefs[SESSION_ID] = it }
                        prefs[CREDENTIALS] = result.data.toStringSet()
                        prefs[LOCAL_CREDENTIAL_ID] = credentialId
                    }
                    signInStateMutable.emit(SignInState.SignedIn(username))
                    refreshCredentials()
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Cannot call registerResponse", e)
        }
    }
}
