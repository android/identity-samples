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

import android.util.Base64
import android.util.Log
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

class SignUpViewModel(private val jsonProvider: JsonProvider) : ViewModel() {
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isPasswordInputVisible = MutableStateFlow(false)
    val isPasswordInputVisible = _isPasswordInputVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _passkeyCreationError = MutableStateFlow<String?>(null)
    val passkeyCreationError = _passkeyCreationError.asStateFlow()

    private val _passwordCreationError = MutableStateFlow<String?>(null)
    val passwordCreationError = _passwordCreationError.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    /**
     * Called when the username changes.
     *
     * @param newUsername The new username.
     */
    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
        _usernameError.value = null
        _passkeyCreationError.value = null
        _passwordCreationError.value = null
    }

    /**
     * Called when the password changes.
     *
     * @param newPassword The new password.
     */
    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _passwordError.value = null
    }

    /**
     * Signs up the user with a passkey.
     *
     * This function initiates the passkey creation process by calling the Credential Manager API.
     *
     * @param createCredential A suspend function that takes a [CreateCredentialRequest] and returns a [CreatePublicKeyCredentialResponse].
     */
    fun signUpWithPasskey(createCredential: suspend (CreateCredentialRequest) -> CreatePublicKeyCredentialResponse) {
        if (_username.value.isBlank()) {
            _usernameError.value = "Username cannot be blank"
            return
        }
        clearErrors()

        viewModelScope.launch {
            _isLoading.value = true

            TODO("Create a CreatePublicKeyCredentialRequest() with necessary registration json from server")

            try {
                TODO("Call createCredential() with createPublicKeyCredentialRequest")
                TODO("Complete the registration process after sending public key credential to your server and let the user in")
            } catch (e: CreateCredentialException) {
                handlePasskeyFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handlePasskeyFailure(e: CreateCredentialException) {
        val msg = when (e) {
            is CreatePublicKeyCredentialDomException -> {
                // Handle the passkey DOM errors thrown according to the
                // WebAuthn spec using e.domError
                "An error occurred while creating a passkey, please check logs for additional details."
            }

            is CreateCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to register the credential.
                "The user intentionally canceled the operation and chose not to register the credential. Check logs for additional details."
            }

            is CreateCredentialInterruptedException -> {
                // Retry-able error. Consider retrying the call.
                "The operation was interrupted, please retry the call. Check logs for additional details."
            }

            is CreateCredentialProviderConfigurationException -> {
                // Your app is missing the provider configuration dependency.
                // Most likely, you're missing "credentials-play-services-auth".
                "Your app is missing the provider configuration dependency. Check logs for additional details."
            }

            is CreateCredentialUnknownException -> {
                "An unknown error occurred while creating passkey. Check logs for additional details."
            }

            is CreateCredentialCustomException -> {
                // You have encountered an error from a 3rd-party SDK. If you
                // make the API call with a request object that's a subclass of
                // CreateCustomCredentialRequest using a 3rd-party SDK, then you
                // should check for any custom exception type constants within
                // that SDK to match with e.type. Otherwise, drop or log the
                // exception.
                "An unknown error occurred from a 3rd party SDK. Check logs for additional details."
            }

            else -> {
                Log.w("Auth", "Unexpected exception type ${e::class.java.name}")
                "An unknown error occurred."
            }
        }
        Log.e("Auth", "createPasskey failed with exception: " + e.message.toString())
        _passkeyCreationError.value = msg
    }

    // Dummy function to simulate server passkey registration.
    private fun registerResponse(): Boolean {
        return true
    }

    /**
     * Signs up the user with a password.
     *
     * This function initiates the password creation process by calling the Credential Manager API.
     *
     * @param createCredential A suspend function that takes a [CreateCredentialRequest] and returns a [CreateCredentialResponse].
     */
    fun signUpWithPassword(createCredential: suspend (CreateCredentialRequest) -> CreateCredentialResponse) {
        if (!_isPasswordInputVisible.value) {
            _isPasswordInputVisible.value = true
            clearErrors()
        } else {
            var valid = true
            if (_username.value.isBlank()) {
                _usernameError.value = "User name required"
                valid = false
            }
            if (_password.value.isBlank()) {
                _passwordError.value = "Password required"
                valid = false
            }
            if (!valid) return

            clearErrors()
            _isLoading.value = true

            viewModelScope.launch {
                TODO("CreatePasswordRequest with entered username and password")

                TODO("Create credential with created password request and log the user in")
            }
        }
    }

    private suspend fun simulateServerDelayAndLogIn() {
        delay(1000) // Simulate server delay
        DataProvider.setSignedInThroughPasskeys(false)
        _isLoading.value = false
        _navigationEvent.emit(NavigationEvent.NavigateToHome(signedInWithPasskeys = false))
    }

    private fun getEncodedUserId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }

    private fun getEncodedChallenge(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }

    private fun clearErrors() {
        _usernameError.value = null
        _passwordError.value = null
        _passkeyCreationError.value = null
        _passwordCreationError.value = null
    }
}
