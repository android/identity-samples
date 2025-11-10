/*
 * Copyright 2025 Google LLC
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

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A view model for the sign-in screen.
 *
 * This view model handles the sign-in process, including calling the Credential Manager API,
 * and exposing the sign-in state to the UI.
 *
 * @param jsonProvider The provider for JSON data.
 */
class SignInViewModel(private val jsonProvider: JsonProvider) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError = _signInError.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    /**
     * Signs in the user.
     *
     * This function initiates the sign-in process by calling the Credential Manager API to
     * retrieve saved credentials.
     *
     * @param getCredential A suspend function that takes a [GetCredentialRequest] and returns a [GetCredentialResponse].
     */
    fun signIn(getCredential: suspend (GetCredentialRequest) -> GetCredentialResponse) {
        _isLoading.value = true
        _signInError.value = null

        viewModelScope.launch {
            TODO("Create a GetPublicKeyCredentialOption() with necessary authentication json from server")

            TODO("Create a PasswordOption to retrieve all the associated user's password")

            TODO("Combine requests into a GetCredentialRequest")

            try {
                TODO("Call getCredential() with required credential options")
                TODO("Complete the authentication process after validating the public key credential to your server and let the user in.")
            } catch (e:Exception) {
                Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
                _signInError.value = e.message.toString()
            } finally {
                _isLoading.value = false
            }

        }
    }

    private fun sendSignInResponseToServer(): Boolean {
        return true
    }
}