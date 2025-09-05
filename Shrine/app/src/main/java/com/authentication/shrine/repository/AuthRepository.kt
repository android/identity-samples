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
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.RestoreCredential
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.authentication.shrine.api.ApiException
import com.authentication.shrine.api.AuthApiService
import com.authentication.shrine.model.AuthError
import com.authentication.shrine.model.AuthResult
import com.authentication.shrine.model.CredmanResponse
import com.authentication.shrine.model.FederationOptionsRequest
import com.authentication.shrine.model.PasskeysList
import com.authentication.shrine.model.PasswordRequest
import com.authentication.shrine.model.RegisterRequestRequestBody
import com.authentication.shrine.model.RegisterResponseRequestBody
import com.authentication.shrine.model.ResponseObject
import com.authentication.shrine.model.SignInResponseRequest
import com.authentication.shrine.model.SignInWithGoogleRequest
import com.authentication.shrine.model.UsernameRequest
import com.authentication.shrine.utility.createCookieHeader
import com.authentication.shrine.utility.getJsonObject
import com.authentication.shrine.utility.getSessionId
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val SERVER_CLIENT_ID =
    "493201854729-bposa1duevdn4nspp28cmn6anucu60pf.apps.googleusercontent.com"

/**
 * Repository class that handles authentication-related operations.
 *
 * @param authApiService The API service for interacting with the server.
 * @param dataStore The data store for storing user data.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authApiService: AuthApiService,
) {

    // Companion object for constants and helper methods
    companion object {
        const val TAG = "AuthRepository"

        // Keys for SharedPreferences
        val USERNAME = stringPreferencesKey("username")
        val IS_SIGNED_IN_THROUGH_PASSKEYS = booleanPreferencesKey("is_signed_passkeys")
        val SESSION_ID = stringPreferencesKey("session_id")
        val RESTORE_KEY_CREDENTIAL_ID = stringPreferencesKey("restore_key_credential_id")

        // Value for restore credential AuthApiService parameter
        const val RESTORE_KEY_TYPE_PARAMETER = "rc"
        const val RESTORE_CREDENTIAL_AAGUID = "restore-credential"

        suspend fun <T> DataStore<Preferences>.read(key: Preferences.Key<T>): T? {
            return data.map { it[key] }.first()
        }
    }

    /**
     * Registers the username with the server.
     *
     * @param username The username to send.
     * @return True if the login was successful, false otherwise.
     */
    suspend fun registerUsername(username: String): AuthResult<Unit> {
        return try {
            val response = authApiService.registerUsername(UsernameRequest(username))
            if (response.isSuccessful) {
                dataStore.edit { prefs ->
                    prefs[USERNAME] = username
                    response.getSessionId()?.also {
                        prefs[SESSION_ID] = it
                    }
                }
                AuthResult.Success(Unit)
            } else {
                if (response.code() == 401) {
                    signOut()
                }
                AuthResult.Failure(AuthError.ServerError(response.message()))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
    }

    /**
     * Sends the username to the server.
     *
     * @param username The username to send.
     * @param password The password to send.
     * @return True if the login was successful, false otherwise.
     */
    suspend fun login(username: String, password: String): AuthResult<Unit> {
        return try {
            val response = authApiService.setUsername(UsernameRequest(username = username))
            if (response.isSuccessful) {
                dataStore.edit { prefs ->
                    prefs[USERNAME] = username
                    response.getSessionId()?.also {
                        prefs[SESSION_ID] = it
                    }
                }
                setSessionWithPassword(password)
                AuthResult.Success(Unit)
            } else {
                if (response.code() == 401) {
                    signOut()
                }
                AuthResult.Failure(AuthError.ServerError(response.message()))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
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
                val response = authApiService.setPassword(
                    cookie = sessionId.createCookieHeader(),
                    password = PasswordRequest(password = password),
                )
                if (response.isSuccessful) {
                    dataStore.edit { prefs ->
                        prefs[USERNAME] = response.body()?.username.orEmpty()
                        response.getSessionId()?.also {
                            prefs[SESSION_ID] = it
                        }
                    }
                    return true
                } else if (response.code() == 401) {
                    signOut()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Invalid login credentials", e)

                // Remove previously stored credentials and start login over again
                signOut()
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
            prefs.remove(RESTORE_KEY_CREDENTIAL_ID)
        }
    }

    /**
     * Starts to register a passkey creation request to the server.
     *
     * @return The public key credential request options, or null if there was an error.
     */
    suspend fun registerPasskeyCreationRequest(): AuthResult<JSONObject> {
        return try {
            val sessionId = dataStore.read(SESSION_ID)
            if (!sessionId.isNullOrEmpty()) {
                val response = authApiService.registerRequest(
                    cookie = sessionId.createCookieHeader(),
                    requestBody = RegisterRequestRequestBody(),
                )
                if (response.isSuccessful) {
                    dataStore.edit { prefs ->
                        response.getSessionId()?.also {
                            prefs[SESSION_ID] = it
                        }
                    }
                    val responseObject = response.getJsonObject()
                    AuthResult.Success(responseObject)
                } else {
                    if (response.code() == 401) {
                        signOut()
                    }
                    AuthResult.Failure(AuthError.ServerError(response.message()))
                }
            } else {
                AuthResult.Failure(AuthError.Unknown(null))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
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
    ): AuthResult<Unit> {
        return try {
            // Field to pass as query parameter to authApiService.
            val typeParam: String?
            val registrationResponseJson = when (credentialResponse) {
                is CreatePublicKeyCredentialResponse -> {
                    typeParam = null
                    JSONObject(credentialResponse.registrationResponseJson)
                }

                is CreateRestoreCredentialResponse -> {
                    typeParam = RESTORE_KEY_TYPE_PARAMETER
                    JSONObject(credentialResponse.responseJson)
                }

                else -> {
                    return AuthResult.Failure(AuthError.Unknown("Unknown credential type"))
                }
            }

            val rawId = registrationResponseJson.getString("rawId")
            val response = registrationResponseJson.getJSONObject("response")
            val sessionId = dataStore.read(SESSION_ID)
            if (!sessionId.isNullOrBlank()) {
                val apiResult = authApiService.registerResponse(
                    cookie = sessionId.createCookieHeader(),
                    type = typeParam,
                    requestBody = RegisterResponseRequestBody(
                        id = rawId,
                        type = PublicKeyCredentialType.PUBLIC_KEY.toString(),
                        rawId = rawId,
                        response = CredmanResponse(
                            clientDataJSON = response.getString("clientDataJSON"),
                            attestationObject = response.getString("attestationObject"),
                        ),
                    ),
                )
                if (apiResult.isSuccessful) {
                    dataStore.edit { prefs ->
                        if (credentialResponse is CreateRestoreCredentialResponse) {
                            prefs[RESTORE_KEY_CREDENTIAL_ID] = rawId
                        }
                        apiResult.getSessionId()?.also {
                            prefs[SESSION_ID] = it
                        }
                    }
                    AuthResult.Success(Unit)
                } else {
                    if (apiResult.code() == 401) {
                        signOut()
                    }
                    AuthResult.Failure(AuthError.ServerError(apiResult.message()))
                }
            } else {
                AuthResult.Failure(AuthError.Unknown(null))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
    }

    /**
     * Starts to sign in with a credential.
     *
     * @return The public key credential request options, or null if there was an error.
     */
    suspend fun signInWithPasskeyOrPasswordRequest(): AuthResult<JSONObject> {
        return try {
            val response = authApiService.signInRequest()
            if (response.isSuccessful) {
                dataStore.edit { prefs ->
                    response.getSessionId()?.also {
                        prefs[SESSION_ID] = it
                    }
                }
                val responseObject = response.getJsonObject()
                AuthResult.Success(responseObject)
            } else {
                if (response.code() == 401) {
                    signOut()
                }
                AuthResult.Failure(AuthError.ServerError(response.message()))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
    }

    /**
     * Finishes to signing in with a credential. This should only be called after a call to
     * [signInWithPasskeyRequest] and a local API for key assertion.
     *
     * @param credentialResponse The credential response.
     * @return True if the sign-in was successful, false otherwise.
     */
    suspend fun signInWithPasskeyOrPasswordResponse(credentialResponse: GetCredentialResponse): AuthResult<Unit> {
        return try {
            val credential = credentialResponse.credential
            if (credential is PublicKeyCredential) {
                val signInResponse =
                    credential.data.getString(
                        if (credential.type == RestoreCredential.TYPE_RESTORE_CREDENTIAL) {
                            "androidx.credentials.BUNDLE_KEY_GET_RESTORE_CREDENTIAL_RESPONSE"
                        } else {
                            "androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON"
                        },
                    )
                if (signInResponse != null) {
                    val signInResponseJSON = JSONObject(signInResponse)
                    val response = signInResponseJSON.getJSONObject("response")
                    val sessionId = dataStore.read(SESSION_ID)
                    val credentialId = signInResponseJSON.getString("rawId")

                    if (!sessionId.isNullOrBlank()) {
                        val apiResult = authApiService.signInResponse(
                            cookie = sessionId.createCookieHeader(),
                            requestBody = SignInResponseRequest(
                                id = credentialId,
                                type = PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                rawId = credentialId,
                                response = ResponseObject(
                                    clientDataJSON = response.getString("clientDataJSON"),
                                    authenticatorData = response.getString("authenticatorData"),
                                    signature = response.getString("signature"),
                                    userHandle = response.getString("userHandle"),
                                ),
                            ),
                        )
                        return if (apiResult.isSuccessful) {
                            dataStore.edit { prefs ->
                                apiResult.getSessionId()?.also {
                                    prefs[SESSION_ID] = it
                                }
                            }
                            AuthResult.Success(Unit)
                        } else {
                            if (apiResult.code() == 401) {
                                signOut()
                            }
                            AuthResult.Failure(AuthError.ServerError(apiResult.message()))
                        }
                    }
                }
            } else if (credential is PasswordCredential) {
                val email =
                    credential.data.getString("androidx.credentials.BUNDLE_KEY_ID")
                val password =
                    credential.data.getString("androidx.credentials.BUNDLE_KEY_PASSWORD")
                if (email != null && password != null) {
                    return login(email, password)
                }
            }
            AuthResult.Failure(AuthError.Unknown(null))
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
    }


    /**
     * Sends the session ID to the server to sign in the user.
     * @param sessionId The session ID retrieved from the server via federation options request.
     * @param credentialResponse The credential retrieved from Credential Manager.
     */
    suspend fun signInWithFederatedTokenResponse(
        sessionId: String,
        credentialResponse: GetCredentialResponse
    ): AuthResult<Unit> {
        return try {
            val credential = credentialResponse.credential
            if (credential is CustomCredential) {
                val isSuccess = verifyIdToken(
                    sessionId,
                    GoogleIdTokenCredential
                        .createFrom(credential.data).idToken
                )
                if (isSuccess) {
                    AuthResult.Success(Unit)
                } else {
                    AuthResult.Failure(AuthError.InvalidCredentials)
                }
            } else {
                Log.e(TAG, "Invalid federated token credential")
                AuthResult.Failure(AuthError.Unknown("Invalid federated token credential"))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
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
     * Checks if session id is valid with server. Currently makes a getKeys() request
     *
     * @return True if the session id is valid, false otherwise.
     */
    suspend fun isSessionIdValid(): AuthResult<Unit> {
        return try {
            val sessionId = dataStore.read(SESSION_ID)
            if (!sessionId.isNullOrBlank()) {
                val apiResult = authApiService.getKeys(
                    cookie = sessionId.createCookieHeader(),
                )
                if (apiResult.isSuccessful) {
                    AuthResult.Success(Unit)
                } else {
                    AuthResult.Failure(AuthError.InvalidCredentials)
                }
            } else {
                AuthResult.Failure(AuthError.Unknown(null))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
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

    /**
     * Clears the stored session ID from the data store asynchronously.
     *
     * This is a suspend function that edits the data store to remove the session ID.
     */
    suspend fun clearSessionIdFromDataStore() {
        dataStore.edit { prefs ->
            prefs.remove(SESSION_ID)
        }
    }

    /**
     * Retrieves the stored username asynchronously.
     *
     * This is a suspend function that reads the username from the data store.
     *
     * @return The stored username as a [String]. Returns an empty string if no username is found.
     */
    suspend fun getUsername(): String {
        return dataStore.read(USERNAME).orEmpty()
    }

    /**
     * Retrieves a list of Passkeys from the Backend
     *
     * @return [PasskeysList] Object holding a list of Passkey details
     * */
    suspend fun getListOfPasskeys(): PasskeysList? {
        val sessionId = dataStore.read(SESSION_ID)
        if (!sessionId.isNullOrBlank()) {
            val apiResult = authApiService.getKeys(
                cookie = sessionId.createCookieHeader(),
            )
            if (apiResult.isSuccessful) {
                return apiResult.body()
            } else if (apiResult.code() == 401) {
                signOut()
                return null
            }
        }
        signOut()
        return null
    }

    /**
     * Deletes a passkey from the Backend
     * @param credentialId The ID of the credential to be deleted
     * @return True if the deletion was successful, false otherwise
     */
    suspend fun deletePasskey(credentialId: String): AuthResult<Unit> {
        val sessionId = dataStore.read(SESSION_ID)
        // Construct endpoint for deleting passkeys.
        return try {
            if (!sessionId.isNullOrEmpty()) {
                val response = authApiService.deletePasskey(
                    cookie = sessionId.createCookieHeader(),
                    credentialId = credentialId,
                )
                if (response.isSuccessful) {
                    AuthResult.Success(Unit)
                } else {
                    if (response.code() == 401) {
                        signOut()
                    }
                    AuthResult.Failure(AuthError.ServerError(response.message()))
                }
            } else {
                AuthResult.Failure(AuthError.Unknown(null))
            }
        } catch (e: IOException) {
            AuthResult.Failure(AuthError.NetworkError)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown(e.message))
        }
    }

    suspend fun deleteRestoreKeyFromServer(): Boolean {
        val sessionId = dataStore.read(SESSION_ID)
        val credentialId = dataStore.read(RESTORE_KEY_CREDENTIAL_ID)
        // Construct endpoint for deleting passkeys.
        try {
            if (!sessionId.isNullOrEmpty() && !credentialId.isNullOrEmpty()) {
                val response = authApiService.deletePasskey(
                    cookie = sessionId.createCookieHeader(),
                    credentialId = credentialId,
                )
                if (response.isSuccessful) {
                    return true
                } else if (response.code() == 401) {
                    signOut()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot call deleteRestoreKey", e)
        }
        return false
    }

    /**
     * Send a request to the server with urls parameter that contains a list of IdPs in an array.
     * e.g. url=["https://accounts.google.com"]. The response will contain a client ID to be used
     * for subsequent server verification to complete login. Note this sequence may vary depending
     * on your server implementation.
     * @return The client ID stored as the session ID as a [String].
     */
    suspend fun getFederationOptions(): String? {
        val apiResult = authApiService.getFederationOptions(FederationOptionsRequest())
        if (apiResult.isSuccessful) {
            return apiResult.getSessionId()
        }

        return null
    }

    /**
     * Verifies the ID token with the server to complete sign in.
     * @param sessionId The ID token retrieved from the server via federation options request. This
     * is treated as a session ID for this server implementation.
     * @param token The ID token to be authorized.
     */
    suspend fun verifyIdToken(sessionId: String, token: String): Boolean {
        val apiResult = authApiService.verifyIdToken(
            cookie = sessionId.createCookieHeader(),
            requestParams = SignInWithGoogleRequest(token = token)
        )

        if (apiResult.isSuccessful) {
            apiResult.getSessionId()?.let { newSessionId ->
                dataStore.edit { prefs ->
                    prefs[SESSION_ID] = newSessionId
                }
                return true
            }
        }

        return false
    }
}
