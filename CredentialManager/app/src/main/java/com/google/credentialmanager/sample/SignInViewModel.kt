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

import android.util.Log
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
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
            val getPublicKeyCredentialOption =
                GetPublicKeyCredentialOption(jsonProvider.fetchAuthJson(), null)

            val getPasswordOption = GetPasswordOption()

            val request = GetCredentialRequest(
                listOf(
                    getPublicKeyCredentialOption,
                    getPasswordOption
                )
            )

            try {
                val result = getCredential(request)

                val data = when (result.credential) {
                    is PublicKeyCredential -> {
                        val cred = result.credential as PublicKeyCredential
                        DataProvider.setSignedInThroughPasskeys(true)
                        "Passkey: ${cred.authenticationResponseJson}"
                    }

                    is PasswordCredential -> {
                        val cred = result.credential as PasswordCredential
                        DataProvider.setSignedInThroughPasskeys(false)
                        "Got Password - User:${cred.id} Password: ${cred.password}"
                    }

                    is CustomCredential -> {
                        //If you are also using any external sign-in libraries, parse them here with the utility functions provided.
                        null
                    }

                    else -> null
                }

                if (data != null) {
                    sendSignInResponseToServer()
                    _navigationEvent.emit(NavigationEvent.NavigateToHome(signedInWithPasskeys = DataProvider.isSignedInThroughPasskeys()))
                }
            } catch (e: Exception) {
                Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
                _signInError.value =
                    "An error occurred while authenticating: " + e.message.toString()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun sendSignInResponseToServer(): Boolean {
        return true
    }
}