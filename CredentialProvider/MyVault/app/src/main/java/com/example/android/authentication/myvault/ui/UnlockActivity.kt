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
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo.Builder
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.CredentialsRepository

/**
 * Activity responsible for coordinating the secure unlock process of the MyVault application.
 * This includes:
 *  * Handling biometric or device credential authentication.
 *  * Processing credential retrieval requests (using the CredentialsRepository).
 *  * Providing an appropriate response to the system after successful authentication.
 */
class UnlockActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MyVault"
    }

    private lateinit var credentialsRepo: CredentialsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        credentialsRepo = CredentialsRepository(
            AppDependencies.sharedPreferences,
            AppDependencies.credentialsDataSource,
            applicationContext,
        )

        val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        if (request != null) {
            unlock(request)
        }
    }

    /**
     * Initiates the biometric unlock process.
     *
     * @param request The BeginGetCredentialRequest obtained from the intent.
     */
    private fun unlock(request: BeginGetCredentialRequest) {
        val biometricPrompt = BiometricPrompt(
            this,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, getString(R.string.authentication_error, errString))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e(TAG, getString(R.string.authentication_failed))
                    finish()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    // applocked to false
                    processGetCredentialRequest(request)
                }
            },
        )
        authenticate(biometricPrompt)
    }

    /**
     * Processes the BeginGetCredentialRequest, generating a response and finishing the activity.
     *
     * @param request The BeginGetCredentialRequest to process.
     */
    private fun processGetCredentialRequest(request: BeginGetCredentialRequest) {
        val authenticationResultIntent = Intent()

        val responseBuilder = BeginGetCredentialResponse.Builder()

        if (credentialsRepo.processGetCredentialsRequest(request, responseBuilder)) {
            PendingIntentHandler.setBeginGetCredentialResponse(
                authenticationResultIntent,
                responseBuilder.build(),
            )
        }
        setResult(RESULT_OK, authenticationResultIntent)
        finish()
    }

    /**
     * Configures and displays the biometric authentication prompt.
     *
     * @param biometricPrompt The BiometricPrompt instance used for authentication.
     */
    private fun authenticate(biometricPrompt: BiometricPrompt) {
        val promptInfo = Builder()
            .setTitle(getString(R.string.unlock_app))
            .setSubtitle(getString(R.string.unlock_app_to_access_credentials))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}
