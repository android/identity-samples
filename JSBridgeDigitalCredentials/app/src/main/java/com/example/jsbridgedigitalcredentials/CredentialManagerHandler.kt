package com.example.jsbridgedigitalcredentials

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialException

@OptIn(ExperimentalDigitalCredentialApi::class)
class CredentialManagerHandler(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getDigitalCredential(requestJson: String): String {
        Log.d(TAG, "getDigitalCredential called with: $requestJson")
        val digitalCredentialOption = GetDigitalCredentialOption(requestJson)
        val getCredRequest = GetCredentialRequest(listOf(digitalCredentialOption))

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = getCredRequest
            )
            val credential = result.credential
            if (credential is DigitalCredential) {
                Log.d(TAG, "Got DigitalCredential: ${credential.credentialJson}")
                credential.credentialJson
            } else {
                Log.e(TAG, "Received unexpected credential type: ${credential.type}")
                "{\"error\": \"Unexpected credential type: ${credential.type}\"}"
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}", e)
            "{\"error\": \"${e.message}\", \"type\": \"${e.type}\"}"
        } catch (e: Exception) {
            Log.e(TAG, "Unknown Exception: ${e.message}", e)
            "{\"error\": \"${e.message}\"}"
        }
    }

    companion object {
        private const val TAG = "CredManHandler"
    }
}
