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
