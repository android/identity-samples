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
package com.example.android.authentication.shrine

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
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

    suspend fun createRestoreKey(
        requestResult: JSONObject,
        context: Context,
    ) {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(context)
        if (passkeysEligibility.isEligible) {
            val restoreCredentialRequest = CreateRestoreCredentialRequest(requestResult.toString())
            try {
                credentialManager.createCredential(
                    context,
                    restoreCredentialRequest,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getRestoreKey(
        authenticationJson: JSONObject,
        activity: Context,
    ): GenericCredentialManagerResponse {
        val passkeysEligibility = PasskeysEligibility.isPasskeySupported(activity)
        if (!passkeysEligibility.isEligible) {
            return GenericCredentialManagerResponse.Error(errorMessage = passkeysEligibility.reason)
        }

        val options = GetRestoreCredentialOption(authenticationJson.toString())
        val getRestoreKeyRequest = GetCredentialRequest(listOf(options))
        val result: GetCredentialResponse?
        try {
            result = credentialManager.getCredential(
                activity,
                getRestoreKeyRequest,
            )
        } catch (e: Exception) {
            return GenericCredentialManagerResponse.Error(errorMessage = e.message ?: "")
        }
        return GenericCredentialManagerResponse.GetPasskeySuccess(result)
    }

    suspend fun deleteRestoreKey() {
        val clearRequest = ClearCredentialStateRequest()
        credentialManager.clearCredentialState(clearRequest)
    }
}

sealed class GenericCredentialManagerResponse {
    data class GetPasskeySuccess(
        val getPasskeyResponse: GetCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class CreatePasskeySuccess(
        val createPasskeyResponse: CreatePublicKeyCredentialResponse,
    ) : GenericCredentialManagerResponse()

    data class Error(
        val errorMessage: String,
    ) : GenericCredentialManagerResponse()
}
