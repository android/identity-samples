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
import androidx.credentials.exceptions.CreateCredentialException
import org.json.JSONObject
import javax.inject.Inject

/**
 * A utility class that provides methods for interacting with the credential manager.
 *
 * @param credentialManager Instance of [CredentialManager]
 */
class CredentialManagerUtils @Inject constructor(
    private val credentialManager: CredentialManager,
) {

    /**
     * Retrieves a passkey from the credential manager.
     *
     * @param creationResult The result of the passkey creation operation.
     * @param context The activity context from the Composable, to be used in Credential Manager APIs
     * @return The [GetCredentialResponse] object containing the passkey, or null if an error occurred.
     */
    suspend fun getPasskey(
        creationResult: JSONObject,
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
                        creationResult.toString(),
                        null,
                    ),
                    GetPasswordOption(),
                ),
            )
            result = credentialManager.getCredential(context, credentialRequest)
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.GetPasskeySuccess(getPasskeyResponse = result)
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
        return GenericCredentialManagerResponse.GetPasskeySuccess(result)
    }

    /**
     * Deletes the restore key using the Credential Manager API.
     */
    suspend fun deleteRestoreKey() {
        val clearRequest = ClearCredentialStateRequest(requestType = TYPE_CLEAR_RESTORE_CREDENTIAL)
        credentialManager.clearCredentialState(clearRequest)
    }
}

sealed class GenericCredentialManagerResponse {
    data class GetPasskeySuccess(
        val getPasskeyResponse: GetCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class CreatePasskeySuccess(
        val createPasskeyResponse: CreateCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class Error(
        val errorMessage: String,
    ) : GenericCredentialManagerResponse()
}
