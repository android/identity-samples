/*
 * Copyright 2025 The Android Open Source Project
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
package com.example.android.authentication.myvault

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.provider.BiometricPromptResult

/**
 * Utility class for handling biometric authentication errors.
 *
 * <p>This class provides a collection of static utility methods for managing
 * and processing errors that may occur during biometric authentication flows.
 * It encapsulates the logic for extracting error information from
 * {@link BiometricPromptResult} objects and constructing user-friendly error
 * messages.
 *
 * <p>The primary function of this class is to centralize the error-handling
 * logic related to biometric authentication, promoting code reuse and
 * maintainability.
 */
object BiometricErrorUtils {

    /**
     * Checks if there was an error during the biometric authentication flow and returns an error message if so.
     *
     * <p>This method determines whether the biometric authentication flow resulted in
     * an error. It checks if the {@link BiometricPromptResult} is null or if the
     * authentication was successful. If neither of these conditions is met, it
     * extracts the error code and message from the {@link BiometricPromptResult},
     * constructs an error message, and returns it.
     *
     * <p>The error message is built using the following format:
     * "Biometric Error Code: [errorCode] [errorMessage] Other providers may be available."
     *
     * @param context               The context used to retrieve string resources.
     * @param biometricPromptResult The result of the biometric authentication prompt.
     * @return An error message if there was an error during the biometric flow, or an empty string otherwise.
     */
    @SuppressLint("StringFormatMatches")
    fun getBiometricErrorMessage(
        context: Context,
        biometricPromptResult: BiometricPromptResult?,
    ): String {
        // If the biometricPromptResult is null, there was no error.
        if (biometricPromptResult == null) return context.getString(R.string.empty)

        // If the biometricPromptResult indicates success, there was no error.
        if (biometricPromptResult.isSuccessful) return context.getString(R.string.empty)

        // Initialize default values for the error code and message.
        var biometricAuthErrorCode = -1
        var biometricAuthErrorMsg = context.getString(R.string.unknown_failure)

        // Check if there is an authentication error in the biometricPromptResult.
        if (biometricPromptResult.authenticationError != null) {
            // Extract the error code and message from the authentication error.
            biometricAuthErrorCode = biometricPromptResult.authenticationError!!.errorCode
            biometricAuthErrorMsg = biometricPromptResult.authenticationError!!.errorMsg.toString()
        }

        // Build the error message to be sent to the client.
        val errorMessage = buildString {
            append(
                context.getString(
                    R.string.biometric_error_code_with_message,
                    biometricAuthErrorCode,
                ),
            )
            append(biometricAuthErrorMsg)
            append(context.getString(R.string.other_providers_error_message))
        }

        // Indicate that there was an error during the biometric flow.
        return errorMessage
    }
}
