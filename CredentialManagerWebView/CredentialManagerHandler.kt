package com.credmanwebtest.webhandler

import android.app.Activity
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*

/**
 * A class that encapsulates the credential manager object and provides simplified APIs for
 * creating and retrieving public key credentials. For other types of credentials follow the
 * documentation https://developer.android.com/training/sign-in/passkeys
 */
class CredentialManagerHandler(private val activity: Activity) {

    private val mCredMan = CredentialManager.create(activity.applicationContext)
    private val TAG = "CredentialManagerHandler"

    /**
     * Encapsulates the create passkey API for credential manager in a less error-prone manner.
     *
     * @param request a create public key credential request JSON required by [CreatePublicKeyCredentialRequest].
     * @return [CreatePublicKeyCredentialResponse] containing the result of the credential creation.
     */
    suspend fun createPasskey(request: String): CreatePublicKeyCredentialResponse {
        val createRequest = CreatePublicKeyCredentialRequest(request)
        try {
            return mCredMan.createCredential(activity, createRequest) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Log.i(TAG, "Error creating credential: ErrMessage: ${e.errorMessage}, ErrType: ${e.type}")
            throw e
        }
    }

    /**
     * Encapsulates the get passkey API for credential manager in a less error-prone manner.
     *
     * @param request a get public key credential request JSON required by [GetCredentialRequest].
     * @return [GetCredentialResponse] containing the result of the credential retrieval.
     */
    suspend fun getPasskey(request: String): GetCredentialResponse {
        val getRequest = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(request, null)))
        try {
            return mCredMan.getCredential(activity, getRequest)
        } catch (e: GetCredentialException) {
            // For error handling use guidance from https://developer.android.com/training/sign-in/passkeys
            Log.i(TAG, "Error retrieving credential: ${e.message}")
            throw e
        }
    }
}