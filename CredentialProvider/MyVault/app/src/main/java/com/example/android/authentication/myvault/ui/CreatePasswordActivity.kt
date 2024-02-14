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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.data.PasswordMetaData
import com.example.android.authentication.myvault.ui.password.PasswordScreen
import kotlinx.coroutines.launch
import java.time.Instant

/*
* This class is responsible for handling the password credential (Passkey) creation request from a Relying Party i.e calling app
 */
class CreatePasswordActivity : ComponentActivity() {
    private val credentialsDataSource = AppDependencies.credentialsDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Access the associated intent and pass it into the PendingIntentHander class to get the ProviderCreateCredentialRequest method.
        val createRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        val accountId = intent.getStringExtra(KEY_ACCOUNT_ID)

        handlePassword(createRequest, accountId)
    }

    /**
     * This method handles the password creation request
     * @param : createRequest : Final request received by the provider after the user has selected a given CreateEntry on the UI.
     * @param accountId : user's unique account id
     */
    private fun handlePassword(
        createRequest: ProviderCreateCredentialRequest?,
        accountId: String?,
    ) {
        if (createRequest != null) {
            if (createRequest.callingRequest is CreatePasswordRequest) {
                val request: CreatePasswordRequest =
                    createRequest.callingRequest as CreatePasswordRequest

                setContent {
                    val coroutineScope = rememberCoroutineScope()
                    PasswordScreen(
                        onSave = {
                            coroutineScope.launch {
                                onSaveClick(request, createRequest, accountId)
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Saves the password and sets the response back to the calling app.
     *
     * @param request The create password request.
     * @param createRequest The provider create credential request.
     * @param accountId The user's unique account ID.
     */
    private suspend fun onSaveClick(
        request: CreatePasswordRequest,
        createRequest: ProviderCreateCredentialRequest,
        accountId: String?,
    ) {
        savePassword(
            request.id,
            request.password,
            createRequest.callingAppInfo.packageName,
            accountId,
        )
        // Set the response back
        val result = Intent()
        val response = CreatePasswordResponse()
        PendingIntentHandler.setCreateCredentialResponse(result, response)
        setResult(RESULT_OK, result)
        this.finish()
    }

    /**
     * Saves the user password in storage.
     *
     * @param username The username.
     * @param password The password.
     * @param callingPackage The package name of the calling app.
     * @param accountId The user's unique account ID.
     */
    private suspend fun savePassword(
        username: String,
        password: String,
        callingPackage: String?,
        accountId: String?,
    ) {
        if (callingPackage == null) {
            return
        }

        if (accountId == null || (accountId != USER_ACCOUNT)) {
            // AccountId was not set
        } else {
            saveUserPassword()
        }

        credentialsDataSource.addNewPassword(
            PasswordMetaData(
                username,
                password,
                callingPackage,
                lastUsedTimeMs = Instant.now().toEpochMilli(),
            ),
        )
    }

    /**
     * Saves the user password in storage.
     */
    private fun saveUserPassword() {
        applicationContext.getSharedPreferences(
            applicationContext.packageName,
            MODE_PRIVATE,
        ).edit().apply {
            putLong(
                KEY_ACCOUNT_LAST_USED_MS,
                Instant.now().toEpochMilli(),
            )
        }.apply()
    }

    companion object {
        const val KEY_ACCOUNT_LAST_USED_MS = "key_account_last_used_ms"
        const val KEY_ACCOUNT_ID = "key_account_id"
        const val USER_ACCOUNT = "user_account"
    }
}
