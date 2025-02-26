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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.BiometricPromptResult
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasswordItem
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * This class is responsible for handling the password credential get request from a Relying Party i.e calling app
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
     * This method processes the incoming GetPassword intent to retrieve password
     * credentials for the calling application. It extracts the request details,
     * checks for biometric flow errors, and then attempts to find a matching
     * password credential in the local data source. If a matching credential is
     * found, it configures the response to be sent back to the calling app.
     */
    private fun handleGetPasswordIntent() {
        // Retrieve the ProviderGetCredentialRequest from the intent using PendingIntentHandler.
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        // Check if a valid request was retrieved.
        if (request != null) {
            // Get the first credential option from the request.
            val option = request.credentialOptions[0]

            // Retrieve the biometric prompt result from the request.
            val biometricPromptResult = request.biometricPromptResult

            // Check if there was an error during the biometric flow. If so, handle the error and return.
            if (isValidBiometricFlowError(biometricPromptResult)) return

            // Check if the credential option is a GetPasswordOption.
            if (option is GetPasswordOption) {
                // Extract the username from the intent's extras.
                val username = intent.getStringExtra(getString(R.string.key_account_id))
                try {
                    val (passwordItem: PasswordItem?, password) = configurePasswordItem(
                        request,
                        username,
                    )

                    // Configure the credential response with the found password item, username, and password.
                    configureCredentialResponse(passwordItem, username, password)
                } catch (e: Exception) {
                    // Handle any exceptions that occur during the process.
                }
            }
        }
    }

    /**
     * Configures the password item and password for a given request and username.
     *
     * <p>This method retrieves the password credentials associated with the calling
     * application's package name and searches for a password item that matches the
     * provided username. If a match is found, it returns the corresponding
     * {@link PasswordItem} and password. If no match is found, it returns a
     * {@code null} {@link PasswordItem} and an empty string for the password.
     *
     * @param request  The {@link ProviderGetCredentialRequest} containing
     *                 information about the calling application.
     * @param username The username to search for in the password credentials.
     * @return A {@link Pair} containing the matching {@link PasswordItem} (or
     *         {@code null} if no match is found) and the corresponding password
     *         (or an empty string if no match is found).
     */
    private fun configurePasswordItem(
        request: ProviderGetCredentialRequest,
        username: String?,
    ): Pair<PasswordItem?, String> {
        // Retrieve the credentials associated with the calling app's package name.
        val credentials =
            credentialsDataSource.credentialsForSite(request.callingAppInfo.packageName)

        // Get the list of passwords from the credentials.
        val passwords = credentials?.passwords
        // Initialize variables to store the found password item and the password.
        var passwordItem: PasswordItem? = null
        // Get an iterator for the list of passwords.
        val it = passwords?.iterator()

        // Initialize the password variable with an empty string.
        var password = getString(R.string.empty)

        // Iterate through the passwords to find a matching username.
        while (it?.hasNext() == true) {
            // Get the current password item.
            val passwordItemCurrent = it.next()
            // Check if the current password item's username matches the requested username.
            if (passwordItemCurrent.username == username) {
                // If a match is found, store the password item and password.
                passwordItem = passwordItemCurrent
                password = passwordItemCurrent.password
                break // Exit the loop since we found a match.
            }
        }
        return Pair(passwordItem, password)
    }

    /**
     * Checks if there was an error during the biometric authentication flow.
     *
     * <p>This method determines whether the biometric authentication flow resulted in
     * an error. It checks if the {@link BiometricPromptResult} is null or if the
     * authentication was successful. If neither of these conditions is met, it
     * extracts the error code and message from the {@link BiometricPromptResult},
     * constructs an error message, and sets up a failure response to be sent to
     * the client.
     *
     * @param biometricPromptResult The result of the biometric authentication prompt.
     * @return True if there was an error during the biometric flow, false otherwise.
     */
    @SuppressLint("StringFormatMatches")
    private fun isValidBiometricFlowError(biometricPromptResult: BiometricPromptResult?): Boolean {
        // If the biometricPromptResult is null, there was no error.
        if (biometricPromptResult == null) return false

        // If the biometricPromptResult indicates success, there was no error.
        if (biometricPromptResult.isSuccessful) return false

        // Initialize default values for the error code and message.
        var biometricAuthErrorCode = -1
        var biometricAuthErrorMsg = getString(R.string.unknown_failure)

        // Check if there is an authentication error in the biometricPromptResult.
        if (biometricPromptResult.authenticationError != null) {
            // Extract the error code and message from the authentication error.
            biometricAuthErrorCode = biometricPromptResult.authenticationError!!.errorCode
            biometricAuthErrorMsg = biometricPromptResult.authenticationError!!.errorMsg.toString()
        }

        // Build the error message to be sent to the client.
        val errorMessage = buildString {
            append(
                getString(
                    R.string.biometric_error_code_with_message,
                    biometricAuthErrorCode,
                ),
            )
            append(biometricAuthErrorMsg)
            append(getString(R.string.other_providers_error_message))
        }

        // Set up the failure response and finish the flow with the constructed error message.
        setUpFailureResponseAndFinish(errorMessage)

        // Indicate that there was an error during the biometric flow.
        return true
    }

    /**
     * Sets up a failure response and finishes the activity.
     *
     * @param message The error message to include in the response.
     */
    private fun setUpFailureResponseAndFinish(message: String) {
        val result = Intent()
        PendingIntentHandler.setGetCredentialException(
            result,
            GetCredentialUnknownException(message),
        )
        setResult(RESULT_OK, result)
        finish()
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
