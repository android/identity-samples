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
package com.authentication.shrinewear.ui

import android.app.Activity
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrinewear.Graph
import com.authentication.shrinewear.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the login screen.
 *
 * @property statusCode The string resource ID representing the current authentication status.
 * Defaults to {@link R.string#credman_status_logged_out}.
 * @property inProgress A boolean indicating whether a login operation is currently in progress.
 * Defaults to false.
 */
data class LoginUiState(
    val statusCode: Int = R.string.credman_status_logged_out,
    val inProgress: Boolean = false,
)

/**
 * ViewModel responsible for managing the login flow using the Credential Manager.
 */
class CredentialManagerViewModel : ViewModel() {
    private val credManAuthenticator = Graph.credentialManagerAuthenticator
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Initiates the login process using the Android Credential Manager.
     *
     * @param context The application context.
     */
    fun login(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inProgress = true)

            try {
                if (credManAuthenticator.signInWithCredentialManager(activity)) {
                    Graph.authenticationStatusCode = R.string.credman_status_authorized
                } else {
                    Graph.authenticationStatusCode = R.string.status_failed
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, INFO_DISMISSED_FALLBACK.format(e.message))
                Graph.authenticationStatusCode = R.string.credman_status_dismissed
            } catch (e: NoCredentialException) {
                Log.e(TAG, ERROR_MISSING_CREDENTIALS)
                Graph.authenticationStatusCode = R.string.credman_status_no_credentials
            } catch (e: Exception) {
                Log.e(TAG, ERROR_UNKNOWN_EXCEPTION.format(e.message))
                Graph.authenticationStatusCode = R.string.credman_status_unknown
            } finally {
                _uiState.value = _uiState.value.copy(
                    inProgress = false,
                    statusCode = Graph.authenticationStatusCode,
                )
            }
        }
    }

    companion object {
        private const val TAG = "CredentialManagerViewModel"
        private const val ERROR_MISSING_CREDENTIALS = "Missing credentials. Verify device SDK>35."
        private const val ERROR_UNKNOWN_EXCEPTION = "Unknown Authentication exception: %s"
        private const val INFO_DISMISSED_FALLBACK =
            "Credential Manager dismissed by user, falling back to legacy authentication. Exception details: %s"
    }
}
