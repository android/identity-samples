/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.credentialmanager.sample

import android.R.drawable
import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import org.json.JSONObject

class Auth(context: Context) {
    private val credMan: CredentialManager

    //Configure and initialize CredentialManager
    init {
        credMan = CredentialManager.create(context)
    }

    suspend fun getPasskey(
        activity: Activity,
        creationResult: JSONObject
    ): GetCredentialResponse? {
        Toast.makeText(activity, "Fetching previously stored credentials", Toast.LENGTH_SHORT)
            .show()
        var result: GetCredentialResponse? = null
        try {
            val cr = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        creationResult.toString(),
                        null
                    ),
                    GetPasswordOption()
                )
            )
            result = credMan.getCredential(activity, cr)
            if (result.credential is PublicKeyCredential) {
                val cred = result.credential as PublicKeyCredential
                Log.i("TAG", "Passkey ${cred.authenticationResponseJson}")
                return result
            }
        } catch (e: Exception) {
            showErrorAlert(activity, e)
        }
        return result
    }

    private fun showErrorAlert(activity: Activity, e: Exception) {
        Builder(activity)
            .setTitle("An error occurred")
            .setMessage(e.message)
            .setNegativeButton("Ok", null)
            .setIcon(drawable.ic_dialog_alert)
            .show()
    }

    suspend fun createPassword(
        username: String,
        password: String,
        activity: Activity
    ): String {
        val cr = CreatePasswordRequest(
            username, password
        )
        return try {
            credMan.createCredential(activity, cr) as CreatePasswordResponse
            "Password created and saved"
        } catch (e: Exception) {
            "Exception $e"
        }
    }

    suspend fun createPasskey(
        activity: Activity,
        requestResult: JSONObject
    ): CreatePublicKeyCredentialResponse? {
        val cr = CreatePublicKeyCredentialRequest(requestResult.toString())
        var ret: CreatePublicKeyCredentialResponse? = null
        try {
            ret = credMan.createCredential(
                activity,
                cr as CreateCredentialRequest
            ) as CreatePublicKeyCredentialResponse
        } catch (e: CreateCredentialException) {

            showErrorAlert(activity, e)

            return null
        }
        return ret
    }

    private fun handleFailure(activity: Activity, e: CreateCredentialException) {
        when (e) {
            is CreatePublicKeyCredentialDomException -> {
                // Handle the passkey DOM errors thrown according to the
                // WebAuthn spec using e.domError
                Log.e("Auth", e.domError.toString())
            }
            is CreateCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to register the credential.
            }
            is CreateCredentialInterruptedException -> {
                // Retry-able error. Consider retrying the call.
            }
            is CreateCredentialProviderConfigurationException -> {
                // Your app is missing the provider configuration dependency.
                // Most likely, you're missing the
                // "credentials-play-services-auth" module.
            }
            is CreateCredentialUnknownException -> {
                Log.w("Auth", e.message.toString())
            }
            is CreateCredentialCustomException -> {
                // You have encountered an error from a 3rd-party SDK. If you
                // make the API call with a request object that's a subclass of
                // CreateCustomCredentialRequest using a 3rd-party SDK, then you
                // should check for any custom exception type constants within
                // that SDK to match with e.type. Otherwise, drop or log the
                // exception.
            }
            else -> Log.w("Auth", "Unexpected exception type ${e::class.java.name}")
        }
    }
}
