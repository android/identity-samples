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
package com.authentication.shrine.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.GenericCredentialManagerResponse
import com.authentication.shrine.R
import com.authentication.shrine.model.AuthError
import com.authentication.shrine.model.AuthResult
import com.authentication.shrine.repository.AuthRepository
import com.authentication.shrine.repository.AuthRepository.Companion.TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val coroutineScope: CoroutineScope,
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
    fun signInWithPasskeyOrPasswordRequest(
        onSuccess: (Boolean) -> Unit,
        getCredential: suspend (JSONObject) -> GenericCredentialManagerResponse,
    ) {
        _uiState.update { AuthenticationUiState(isLoading = true) }
        viewModelScope.launch {
            when (val result = repository.signInWithPasskeyOrPasswordRequest()) {
                is AuthResult.Success -> {
                    val credentialResponse = getCredential(result.data)
                    if (credentialResponse is GenericCredentialManagerResponse.GetCredentialSuccess) {
                        signInWithPasskeyOrPasswordResponse(
                            credentialResponse.getCredentialResponse,
                            onSuccess,
                        )
                    } else if (credentialResponse is GenericCredentialManagerResponse.Error) {
                        repository.clearSessionIdFromDataStore()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                passkeyRequestErrorMessage = credentialResponse.errorMessage,
                            )
                        }
                    } else if (credentialResponse is GenericCredentialManagerResponse.CancellationError) {
                        repository.clearSessionIdFromDataStore()
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }

                is AuthResult.Failure -> {
                    var errorMessage: String? = null
                    val messageResId = when (val error = result.error) {
                        is AuthError.NetworkError -> R.string.error_network
                        is AuthError.ServerError -> R.string.error_server
                        is AuthError.Unknown -> {
                            errorMessage = error.message
                            R.string.error_unknown
                        }
                        else -> R.string.error_unknown
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passkeyResponseMessageResourceId = messageResId,
                            passkeyRequestErrorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Handles the response to a sign-in challenge.
     *
     * @param response The response from the server
     * @param onSuccess Lambda that handles actions on successful passkey sign-in
     */
    private fun signInWithPasskeyOrPasswordResponse(
        response: GetCredentialResponse,
        onSuccess: (navigateToHome: Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            when (repository.signInWithPasskeyOrPasswordResponse(response)) {
                is AuthResult.Success -> {
                    val isPasswordCredential = response.credential is PasswordCredential
                    repository.setSignedInState(!isPasswordCredential)
                    _uiState.update {
                        it.copy(
                            isSignInWithPasskeysSuccess = true,
                            isLoading = false
                        )
                    }
                    onSuccess(isPasswordCredential)
                }

                is AuthResult.Failure -> {
                    repository.setSignedInState(false)
                    repository.clearSessionIdFromDataStore()
                    _uiState.update {
                        it.copy(
                            passkeyResponseMessageResourceId = R.string.error_invalid_credentials,
                            isSignInWithPasskeysSuccess = false,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    /**
     * Launches the Sign in with Google authentication flow.
     * @param onSuccess Lambda that handles actions on successful Google sign in
     * @param getCredential Lambda that retrieves the credential from the Credential Manager
     */
    fun signInWithGoogleRequest(
        onSuccess: (Boolean) -> Unit,
        getCredential: suspend () -> GenericCredentialManagerResponse,
    ) {
        _uiState.update { AuthenticationUiState(isLoading = true) }
        viewModelScope.launch {
            val credentialResponse = getCredential()
            if (credentialResponse is GenericCredentialManagerResponse.GetCredentialSuccess) {
                logInWithFederatedToken(
                    credentialResponse.getCredentialResponse,
                    onSuccess,
                )
            } else if (credentialResponse is GenericCredentialManagerResponse.Error) {
                repository.clearSessionIdFromDataStore()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        signInWithGoogleRequestErrorMessage = credentialResponse.errorMessage,
                    )
                }
            } else if (credentialResponse is GenericCredentialManagerResponse.CancellationError) {
                repository.clearSessionIdFromDataStore()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Logs in the user with federated credentials received from Credential Manager.
     * @response Credentials received from Credential Manager
     * @onSuccess Lambda that handles actions on successful Google sign in
     */
    fun logInWithFederatedToken(
        response: GetCredentialResponse,
        onSuccess: (navigateToHome: Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            // Get sessionId from the server first.
            val sessionId = repository.getFederationOptions()
            if (sessionId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        logInWithFederatedTokenFailure = true,
                    )
                }
            } else {
                // Log in to server with retrieved session ID and CredMan credentials.
                when (repository.signInWithFederatedTokenResponse(sessionId, response)) {
                    is AuthResult.Success -> {
                        repository.setSignedInState(flag = false)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                            )
                        }
                        onSuccess(true)
                    }

                    is AuthResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                logInWithFederatedTokenFailure = true,
                            )
                        }
                    }
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
     * @see signInWithPasskeyResponse
     */
    fun checkForStoredRestoreKey(
        getRestoreKey: suspend (JSONObject) -> GenericCredentialManagerResponse,
        onSuccess: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            if (!repository.isSignedInThroughPasskeys() && !repository.isSignedInThroughPassword()) {
                when (val result = repository.signInWithPasskeyOrPasswordRequest()) {
                    is AuthResult.Success -> {
                        val restoreKeyResponse = getRestoreKey(result.data)
                        if (restoreKeyResponse is GenericCredentialManagerResponse.GetCredentialSuccess) {
                            _uiState.update {
                                AuthenticationUiState(isLoading = true)
                            }
                            signInWithPasskeyOrPasswordResponse(
                                response = restoreKeyResponse.getCredentialResponse,
                                onSuccess = onSuccess,
                            )
                        } else {
                            repository.clearSessionIdFromDataStore()
                        }
                    }

                    is AuthResult.Failure -> {
                        repository.clearSessionIdFromDataStore()
                    }
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
        createRestoreKeyOnCredMan: suspend (createRestoreCredRequestObj: JSONObject) -> GenericCredentialManagerResponse,
    ) {
        coroutineScope.launch {
            when (val result = repository.registerPasskeyCreationRequest()) {
                is AuthResult.Success -> {
                    val createRestoreKeyResponse = createRestoreKeyOnCredMan(result.data)
                    if (createRestoreKeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                        repository.registerPasskeyCreationResponse(createRestoreKeyResponse.createPasskeyResponse)
                    }
                }

                is AuthResult.Failure -> {
                    Log.e(TAG, "Error creating restore key.")
                    // Don't block user sign in if this fails.
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
    val signInWithGoogleRequestErrorMessage: String? = null,
    val logInWithFederatedTokenFailure: Boolean = false,
    val isRestoreCredentialFound: Boolean = false,
)
