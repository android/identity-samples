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
package com.example.android.authentication.shrine.ui.viewmodel

import androidx.annotation.StringRes
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.authentication.shrine.GenericCredentialManagerResponse
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * A ViewModel that handles authentication-related operations.
 *
 * This ViewModel is responsible for:
 *
 * - Logging in the user with a username and password.
 * - Requesting a sign-in challenge from the server.
 * - Handling the response to a sign-in challenge.
 *
 * The ViewModel maintains an [AuthenticationUiState] object that represents the current UI state of the
 * authentication screen.
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    /**
     * The UI state of the authentication screen.
     */
    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Requests a sign-in challenge from the server.
     *
     * @param onSuccess Lambda that handles actions on successful passkey sign-in
     * @param getPasskey Lambda that calls CredManUtil's getPasskey method with Activity reference
     */
    fun signInWithPasskeysRequest(
        onSuccess: (Boolean) -> Unit,
        getPasskey: suspend (JSONObject) -> GenericCredentialManagerResponse,
    ) {
        _uiState.value = AuthenticationUiState(isLoading = true)
        viewModelScope.launch {
            repository.signInWithPasskeysRequest()?.let { data ->
                val passkeyResponse = getPasskey(data)
                if (passkeyResponse is GenericCredentialManagerResponse.GetPasskeySuccess) {
                    signInWithPasskeysResponse(
                        passkeyResponse.getPasskeyResponse,
                        onSuccess,
                    )
                } else if (passkeyResponse is GenericCredentialManagerResponse.Error) {
                    _uiState.update {
                        AuthenticationUiState(
                            passkeyRequestErrorMessage = passkeyResponse.errorMessage,
                        )
                    }
                }
            }
        }
    }

    /**
     * Handles the response to a sign-in challenge.
     *
     * @param response The response from the server.
     * @param onSuccess Lambda that handles actions on successful passkey sign-in
     */
    private fun signInWithPasskeysResponse(
        response: GetCredentialResponse,
        onSuccess: (navigateToHome: Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val isSuccess = repository.signInWithPasskeysResponse(response)
            if (isSuccess) {
                val isPasswordCredential = response.credential is PasswordCredential
                repository.setSignedInState(!isPasswordCredential)
                onSuccess(isPasswordCredential)

                _uiState.update {
                    AuthenticationUiState(
                        isSignInWithPasskeysSuccess = true,
                    )
                }
            } else {
                repository.setSignedInState(false)
                _uiState.update {
                    AuthenticationUiState(
                        passkeyResponseMessageResourceId = R.string.some_error_occurred_please_check_logs,
                        isSignInWithPasskeysSuccess = false,
                    )
                }
            }
        }
    }

    /**
     * Checks for a stored restore key and attempts to sign in with it if found.
     *
     * @param getRestoreKey A suspend function that takes a [JSONObject] and returns a [GenericCredentialManagerResponse].
     * This function is responsible for retrieving the restore key from the CredentialManager.
     *
     * @param onSuccess A lambda that takes a [Boolean] indicating the success of the sign-in operation.
     *
     * @see GenericCredentialManagerResponse
     * @see signInWithPasskeysResponse
     */
    fun checkForStoredRestoreKey(
        getRestoreKey: suspend (JSONObject) -> GenericCredentialManagerResponse,
        onSuccess: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.signInWithPasskeysRequest()?.let { data ->
                val restoreKeyResponse = getRestoreKey(data)
                if (restoreKeyResponse is GenericCredentialManagerResponse.GetPasskeySuccess) {
                    _uiState.update {
                        AuthenticationUiState(isLoading = true)
                    }
                    signInWithPasskeysResponse(
                        response = restoreKeyResponse.getPasskeyResponse,
                        onSuccess = onSuccess
                    )
                } else {
                    repository.clearSessionIdFromDataStore()
                }
            }
        }
    }

    /**
     * Creates a restore key by registering a new passkey.
     *
     * @param createRestoreKeyOnCredMan A suspend function that takes a [JSONObject] and returns a
     * [GenericCredentialManagerResponse]. This function is responsible for creating
     * the restore key.
     *
     * @see GenericCredentialManagerResponse
     */
    fun createRestoreKey(
        createRestoreKeyOnCredMan: suspend (createRestoreCredRequestObj: JSONObject) -> GenericCredentialManagerResponse
    ) {
        viewModelScope.launch {
            repository.registerPasskeyCreationRequest()?.let { data ->
                val createRestoreKeyResponse = createRestoreKeyOnCredMan(data)
                if (createRestoreKeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                    repository.registerPasskeyCreationResponse(createRestoreKeyResponse.createPasskeyResponse)
                }
            }
        }
    }
}

/**
 * Data class that stores tha data of Authentication Screen
 */
data class AuthenticationUiState(
    val isLoading: Boolean = false,
    @StringRes val passkeyResponseMessageResourceId: Int = R.string.empty_string,
    val passkeyRequestErrorMessage: String? = null,
    val isSignInWithPasskeysSuccess: Boolean = false,
    val isRestoreCredentialFound: Boolean = false,
)
