package com.androidauth.shrineWear

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.lifecycle.Lifecycle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.delay


enum class CredentialType {
    PASSKEY,
    PASSWORD,
    SIWG,
}

class CredentialManagerAuthenticator(context: Context) {
    private val authenticationServer = AuthenticationServer()
    private val credMan: CredentialManager = CredentialManager.create(context)

    suspend fun signInWithCredentialManager(
        activity: Context,
        types: List<CredentialType> = CredentialType.entries,
    ): Boolean {
        val publicKeyServerParams = authenticationServer.getPublicKeyServerParameters()
        val getCredentialResponse = credMan.getCredential(
            activity,
            createGetCredentialRequest(types, publicKeyServerParams.json!!)
        )

        // The following is a workaround to address a temporary bug where the 'getCredential' call
        // above is still finishing up even though this activity has now resumed.
        // It is necessary to attempt to process the 'getCredentialResponse' to determine if
        // the request has completed. We recommend the progressive delay strategy seen here.
        val componentActivity = (activity as? ComponentActivity)
        var isResponseProcessed = false
        for (i in 1..3) {
            try {
                val isActivityResumed = componentActivity?.lifecycle?.currentState?.isAtLeast(
                    Lifecycle.State.RESUMED) == true

                if (isActivityResumed) {
                    isResponseProcessed = processCredentialResponse(
                        getCredentialResponse, publicKeyServerParams.cookie!!)
                    if (isResponseProcessed) break
                } else {
                    delay(1000L * i)
                }
            } catch (e: FirebaseFunctionsException) {
                Log.w(TAG, "Waiting for credentials from server, attempt ${i} of 3")
            }
        }

        return isResponseProcessed
    }

    private fun createGetCredentialRequest(
        types: List<CredentialType> = CredentialType.entries,
        requestJSON: String
    ): GetCredentialRequest {
        val userCredentialOptions = types.map {
            when (it) {
                CredentialType.PASSKEY -> GetPublicKeyCredentialOption(requestJSON)
                CredentialType.PASSWORD -> GetPasswordOption(isAutoSelectAllowed = false)
                CredentialType.SIWG -> authenticationServer.createGetGoogleIdOption(autoSelect = false)
            }
        }
        return GetCredentialRequest(userCredentialOptions)
    }

    private suspend fun processCredentialResponse(
        getCredentialResponse: GetCredentialResponse,
        cookie: String
    ): Boolean {
        when (val credential = getCredentialResponse.credential) {
            is PasswordCredential -> {
                return authenticationServer.loginWithPassword(credential.id, credential.password)
            }

            is PublicKeyCredential -> {
                return authenticationServer.loginWithPasskey(
                    credential.authenticationResponseJson, cookie
                )
            }

            is CustomCredential -> {
                if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Log.e(TAG, "Unrecognized CustomCredential: ${credential.type}")
                    return false
                }
                Log.i(TAG, "Converting CustomCredential to GoogleIdTokenCredential")
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return authenticationServer.loginWithGoogleToken(googleIdTokenCredential.idToken)
            }

            else -> {
                Log.w(TAG, "Unknown type: ${credential.javaClass.simpleName}")
                return false
            }
        }
    }

    suspend fun registerAuthenticatedGoogleToken(token: String) {
        try {
            authenticationServer.loginWithGoogleToken(token)
        } catch (e: FirebaseFunctionsException) {
            Log.e(TAG, "Signed in, but failed to register legacy Google Sign in account to" +
                    "app credential repository. Error: ${e.message}")
        }
    }

    fun signOut() {
        authenticationServer.signOut()
    }

    companion object {
        private const val TAG = "AuthHandler"
    }
}