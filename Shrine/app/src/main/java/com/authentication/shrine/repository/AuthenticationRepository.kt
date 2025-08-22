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
package com.authentication.shrine.repository

import androidx.credentials.CreateCredentialResponse
import androidx.credentials.GetCredentialResponse
import com.authentication.shrine.model.PasskeysList
import org.json.JSONObject

interface AuthenticationRepository {

    suspend fun registerUsername(username: String): Boolean

    suspend fun login(username: String, password: String): Boolean

    suspend fun signOut()

    suspend fun registerPasskeyCreationRequest(): JSONObject?

    suspend fun registerPasskeyCreationResponse(credentialResponse: CreateCredentialResponse): Boolean

    suspend fun signInWithPasskeyOrPasswordRequest(): JSONObject?

    suspend fun signInWithPasskeyOrPasswordResponse(credentialResponse: GetCredentialResponse): Boolean

    suspend fun isSignedInThroughPassword(): Boolean

    suspend fun isSignedInThroughPasskeys(): Boolean

    suspend fun setSignedInState(flag: Boolean)

    suspend fun clearSessionIdFromDataStore()

    suspend fun getUsername(): String

    suspend fun getListOfPasskeys(): PasskeysList?

    suspend fun deletePasskey(credentialId: String): Boolean

    suspend fun deleteRestoreKeyFromServer(): Boolean

    suspend fun signInWithFederatedTokenResponse(sessionId: String, credentialResponse: GetCredentialResponse): Boolean

    suspend fun getFederationOptions(): String?
}
