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
package com.authentication.shrinewear.ui.viewmodel

import android.app.Activity
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrinewear.AuthenticationState
import com.authentication.shrinewear.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the login flow using the Credential Manager.
 */
class CredentialManagerViewModel : ViewModel() {
    private val credManAuthenticator = Graph.credentialManagerAuthenticator
    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress.asStateFlow()

    /**
     * Initiates the login process using the Android Credential Manager.
     *
     * @param context The application context.
     */
    fun login(activity: Activity) {
        viewModelScope.launch {
            _inProgress.value = true

            try {
                if (credManAuthenticator.signInWithCredentialManager(activity)) {
                    Graph.updateAuthenticationState(AuthenticationState.LOGGED_IN)
                } else {
                    Graph.updateAuthenticationState(AuthenticationState.FAILED)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(
                    TAG,
                    "Dismissed, launching old authentication. Exception: %s".format(e.message),
                )
                Graph.updateAuthenticationState(AuthenticationState.DISMISSED_BY_USER)
            } catch (_: NoCredentialException) {
                Log.e(TAG, "Missing credentials. Verify device SDK>35.")
                Graph.updateAuthenticationState(AuthenticationState.MISSING_CREDENTIALS)
            } catch (e: Exception) {
                Log.e(TAG, "Unknown Authentication exception: %s".format(e.message))
                Graph.updateAuthenticationState(AuthenticationState.UNKNOWN_ERROR)
            } finally {
                _inProgress.value = false
            }
        }
    }

    companion object {
        private const val TAG = "CredentialManagerViewModel"
    }
}
