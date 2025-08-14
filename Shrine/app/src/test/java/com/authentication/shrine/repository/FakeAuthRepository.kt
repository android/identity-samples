package com.authentication.shrine.repository

import androidx.credentials.CreateCredentialResponse
import androidx.credentials.GetCredentialResponse
import com.authentication.shrine.model.PasskeysList
import org.json.JSONObject

class FakeAuthRepository: AuthenticationRepository {
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
        credentialResponse: GetCredentialResponse
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getFederationOptions(): String? {
        TODO("Not yet implemented")
    }
}
