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
package com.example.android.authentication.myvault.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.PendingIntentHandler
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasswordItem
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * This class is responsible for handling the password credential (Passkey) get request from a Relying Party i.e calling app
 */
class GetPasswordActivity : ComponentActivity() {
    private val credentialsDataSource = AppDependencies.credentialsDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleGetPasswordIntent()
    }

    /**
     * This method handles the GetPassword intent received from the calling app.
     *
     * @param getRequest The GetCredentialRequest object containing the request details.
     */
    private fun handleGetPasswordIntent() {
        val getRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        if (getRequest != null) {
            val option = getRequest.credentialOptions[0]

            // Use the GetPasswordOption to retrieve password credentials for the incoming package name.
            if (option is GetPasswordOption) {
                val username = intent.getStringExtra(getString(R.string.key_account_id))
                try {
                    val credentials =
                        credentialsDataSource.credentialsForSite(getRequest.callingAppInfo.packageName)

                    val passwords = credentials?.passwords
                    var passwordItem: PasswordItem? = null
                    val it = passwords?.iterator()

                    var password = getString(R.string.empty)

                    // Check in database if request password credential exist and return associated metadata
                    while (it?.hasNext() == true) {
                        val passwordItemCurrent = it.next()
                        if (passwordItemCurrent.username == username) {
                            passwordItem = passwordItemCurrent
                            password = passwordItemCurrent.password
                            break
                        }
                    }

                    configureCredentialResponse(passwordItem, username, password)
                } catch (e: Exception) {
                    // Catch exception
                }
            }
        }
    }

    /**
     * This method configures the password credential response to be sent back to the calling app.
     *
     * @param passwordItem The PasswordItem object containing the password credential details.
     * @param username The username associated with the password credential.
     * @param password The password associated with the password credential.
     */
    private fun configureCredentialResponse(
        passwordItem: PasswordItem?,
        username: String?,
        password: String,
    ) {
        if (passwordItem == null) {
            val result = Intent()
            PendingIntentHandler.setGetCredentialException(
                result,
                NoCredentialException(),
            )
            setResult(RESULT_OK, result)
            this.finish()
        } else {
            // Update timestamp
            runBlocking {
                credentialsDataSource.updatePassword(
                    passwordItem.copy(
                        lastUsedTimeMs = Instant.now().toEpochMilli(),
                    ),
                )
            }

            setIntentForGetCredentialResponse(username, password)
        }
    }

    /**
     * This method sets the response for the selected password credential and finishes the flow.
     *
     * @param username The username associated with the password credential.
     * @param password The password associated with the password credential.
     */
    private fun setIntentForGetCredentialResponse(username: String?, password: String) {
        val result = Intent()
        val response = PasswordCredential(username.toString(), password)
        PendingIntentHandler.setGetCredentialResponse(
            result,
            GetCredentialResponse(response),
        )
        setResult(RESULT_OK, result)
        this.finish()
    }
}
