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

class FakeAuthRepository : AuthenticationRepository {
    override suspend fun registerUsername(username: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(username: String, password: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun signOut() {
        TODO("Not yet implemented")
    }

    override suspend fun registerPasskeyCreationRequest(): JSONObject? {
        TODO("Not yet implemented")
    }

    override suspend fun registerPasskeyCreationResponse(credentialResponse: CreateCredentialResponse): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithPasskeyOrPasswordRequest(): JSONObject? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithPasskeyOrPasswordResponse(credentialResponse: GetCredentialResponse): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isSignedInThroughPassword(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isSignedInThroughPasskeys(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun setSignedInState(flag: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun clearSessionIdFromDataStore() {
        TODO("Not yet implemented")
    }

    override suspend fun getUsername(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getListOfPasskeys(): PasskeysList? {
        TODO("Not yet implemented")
    }

    override suspend fun deletePasskey(credentialId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRestoreKeyFromServer(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithFederatedTokenResponse(
        sessionId: String,
        credentialResponse: GetCredentialResponse,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getFederationOptions(): String? {
        TODO("Not yet implemented")
    }
}
