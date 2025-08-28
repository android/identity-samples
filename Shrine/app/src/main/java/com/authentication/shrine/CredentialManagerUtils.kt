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

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.SignalAllAcceptedCredentialIdsRequest
import androidx.credentials.SignalCurrentUserDetailsRequest
import androidx.credentials.SignalUnknownCredentialRequest
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.authentication.shrine.repository.AuthRepository.Companion.RP_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.USER_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.read
import com.authentication.shrine.repository.SERVER_CLIENT_ID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import org.json.JSONObject
import javax.inject.Inject

/**
 * A utility class that provides methods for interacting with the credential manager.
 *
 * @param credentialManager Instance of [CredentialManager]
 */
class CredentialManagerUtils @Inject constructor(
    private val credentialManager: CredentialManager,
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Retrieves a passkey or password credential from the credential manager.
     *
     * @param publicKeyCredentialRequestOptions The public key credential request options.
     * @param context The activity context from the Composable, to be used in Credential Manager APIs
     * @return The [GenericCredentialManagerResponse] object containing the passkey or password, or
     * null if an error occurred.
     */
    suspend fun getPasskeyOrPasswordCredential(
        publicKeyCredentialRequestOptions: JSONObject,
        context: Context,
    ): GenericCredentialManagerResponse {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(context)
        if (!passkeysEligibility.isEligible) {
            return GenericCredentialManagerResponse.Error(errorMessage = passkeysEligibility.reason)
        }

        val result: GetCredentialResponse?
        try {
            val credentialRequest = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        publicKeyCredentialRequestOptions.toString(),
                        null,
                    ),
                    GetPasswordOption(),
                ),
            )
            result = credentialManager.getCredential(context, credentialRequest)
        } catch (e: GetCredentialCancellationException) {
            // When credential selector bottom-sheet is cancelled
            return GenericCredentialManagerResponse.CancellationError
        } catch (e: GetPublicKeyCredentialDomException) {
            // When the user verification / biometric bottom sheet is cancelled
            return GenericCredentialManagerResponse.CancellationError
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.GetCredentialSuccess(getCredentialResponse = result)
    }

    /**
     * Retrieves a Sign in with Google credential from the credential manager.
     *
     * @param context The activity context from the Composable, to be used in Credential Manager
     * APIs
     * @return The [GenericCredentialManagerResponse] object containing the passkey or password, or
     * null if an error occurred.
     */
    suspend fun getSignInWithGoogleCredential(context: Context): GenericCredentialManagerResponse {
        val result: GetCredentialResponse?
        try {
            val credentialRequest = GetCredentialRequest(
                listOf(
                    GetGoogleIdOption.Builder()
                        .setServerClientId(SERVER_CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build(),
                ),
            )
            result = credentialManager.getCredential(context, credentialRequest)
        } catch (e: GetCredentialCancellationException) {
            // When credential selector bottom-sheet is cancelled
            return GenericCredentialManagerResponse.CancellationError
        } catch (e: GetPublicKeyCredentialDomException) {
            // When the user verification / biometric bottom sheet is cancelled
            return GenericCredentialManagerResponse.CancellationError
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.GetCredentialSuccess(getCredentialResponse = result)
    }

    /**
     * Creates a new password credential.
     *
     * @param username The username for the new credential.
     * @param password The password for the new credential.
     * @param context The activity context from the Composable, to be used in Credential Manager APIs
     */
    suspend fun createPassword(
        username: String,
        password: String,
        context: Context,
    ) {
        val createPasswordRequest = CreatePasswordRequest(
            username,
            password,
        )
        try {
            credentialManager.createCredential(context, createPasswordRequest) as CreatePasswordResponse
        } catch (_: Exception) { }
    }

    /**
     * Creates a new passkey credential.
     *
     * @param requestResult The result of the passkey creation request.
     * @param context The activity context from the Composable, to be used in Credential Manager APIs
     * @return The [CreatePublicKeyCredentialResponse] object containing the passkey, or null if an error occurred.
     */
    @SuppressLint("PublicKeyCredential")
    suspend fun createPasskey(
        requestResult: JSONObject,
        context: Context,
    ): GenericCredentialManagerResponse {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(context)
        if (!passkeysEligibility.isEligible) {
            return GenericCredentialManagerResponse.Error(errorMessage = passkeysEligibility.reason)
        }

        val credentialRequest = CreatePublicKeyCredentialRequest(requestResult.toString())
        val credentialResponse: CreatePublicKeyCredentialResponse
        try {
            credentialResponse = credentialManager.createCredential(
                context,
                credentialRequest as CreateCredentialRequest,
            ) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialException) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.CreatePasskeySuccess(createPasskeyResponse = credentialResponse)
    }

    /**
     * Creates a restore key using the Credential Manager API.
     *
     * @param requestResult A [JSONObject] containing the data required for creating the restore
     * credential. This data is used to create a [CreateRestoreCredentialRequest]
     *
     * @param context The Android Context used for checking passkey eligibility and interacting
     * with the Credential Manager API.
     *
     * @return A [CreateRestoreCredentialResponse] indicating the result of the operation
     *
     * @throws Exception If any error occurs during the credential creation process.
     */
    suspend fun createRestoreKey(
        requestResult: JSONObject,
        context: Context,
    ): GenericCredentialManagerResponse {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(context)
        val credentialResponse: CreateRestoreCredentialResponse

        if (!passkeysEligibility.isEligible) {
            return GenericCredentialManagerResponse.Error(errorMessage = passkeysEligibility.reason)
        }

        val restoreCredentialRequest = CreateRestoreCredentialRequest(requestResult.toString())
        try {
            credentialResponse = credentialManager.createCredential(
                context,
                restoreCredentialRequest,
            ) as CreateRestoreCredentialResponse
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.CreatePasskeySuccess(createPasskeyResponse = credentialResponse)
    }

    /**
     * Retrieves the restore key using the Credential Manager API.
     *
     * @param authenticationJson The JSON object containing authentication information.
     * @param context The application context.
     * @return A [GenericCredentialManagerResponse] object indicating success or failure.
     */
    suspend fun getRestoreKey(
        authenticationJson: JSONObject,
        context: Context,
    ): GenericCredentialManagerResponse {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(context)
        if (!passkeysEligibility.isEligible) {
            return GenericCredentialManagerResponse.Error(errorMessage = passkeysEligibility.reason)
        }

        val options = GetRestoreCredentialOption(authenticationJson.toString())
        val getRestoreKeyRequest = GetCredentialRequest(listOf(options))
        val result: GetCredentialResponse?
        try {
            result = credentialManager.getCredential(
                context,
                getRestoreKeyRequest,
            )
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.GetCredentialSuccess(result)
    }

    /**
     * Deletes the restore key using the Credential Manager API.
     */
    suspend fun deleteRestoreKey() {
        val clearRequest = ClearCredentialStateRequest(requestType = TYPE_CLEAR_RESTORE_CREDENTIAL)
        credentialManager.clearCredentialState(clearRequest)
    }

    @SuppressLint("RestrictedApi")
    suspend fun signalUnknown(
        credentialId: String,
    ) {
        credentialManager.signalCredentialState(
            SignalUnknownCredentialRequest(
                """{"rpId":"${dataStore.read(RP_ID_KEY)}", "credentialId":"$credentialId"}""",
            ),
        )
    }

    @SuppressLint("RestrictedApi")
    suspend fun signalAcceptedIds(
        credentialIds: List<String>,
    ) {
        credentialManager.signalCredentialState(
            SignalAllAcceptedCredentialIdsRequest(
                """{"rpId":"${dataStore.read(RP_ID_KEY)}","userId":"${dataStore.read(USER_ID_KEY)}","allAcceptedCredentialIds":["${credentialIds.joinToString(",")}"]}""",
            ),
        )
    }

    @SuppressLint("RestrictedApi")
    suspend fun signalUserDetails(
        newName: String,
        newDisplayName: String,
    ) {
        credentialManager.signalCredentialState(
            SignalCurrentUserDetailsRequest(
                """{"rpId":"${dataStore.read(RP_ID_KEY)}","userId":"${dataStore.read(USER_ID_KEY)}", "name":"$newName","displayName":"$newDisplayName"}""",
            ),
        )
    }
}

sealed class GenericCredentialManagerResponse {
    data class GetCredentialSuccess(
        val getCredentialResponse: GetCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class CreatePasskeySuccess(
        val createPasskeyResponse: CreateCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class Error(
        val errorMessage: String,
    ) : GenericCredentialManagerResponse()

    data object CancellationError : GenericCredentialManagerResponse()
}
