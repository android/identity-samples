/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law lifeboatress or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.authentication.shrine.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.GenericCredentialManagerResponse
import com.authentication.shrine.R
import com.authentication.shrine.model.AuthError
import com.authentication.shrine.model.AuthResult
import com.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * ViewModel for user passkey and password registration. Uses [AuthRepository] to interact with the
 * authentication backend
 *
 * @param repository The authentication repository.
 */
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    /**
     * UI state for the registration screen.
     */
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Registers a new user with a passkey.
     *
     * @param username The username of the new user.
     * @param displayName The display name of the new user.
     * @param onSuccess Lambda to be invoked when the registration is successful.
     * @param createPasskeyCallback Lambda to be invoked after login is successful, usually to
     * navigate to the next screen.
     */
    fun onPasskeyRegister(
        username: String,
        displayName: String,
        onSuccess: (navigateToHome: Boolean) -> Unit,
        createPasskeyCallback: suspend (JSONObject) -> GenericCredentialManagerResponse,
    ) {
        _uiState.update { it.copy(isLoading = true) }

        if (username.isNotEmpty() && displayName.isNotEmpty()) {
            viewModelScope.launch {
                when (val result = repository.registerUsername(username, displayName)) {
                    is AuthResult.Success -> {
                        // Now create the passkey and register with the server
                        createPasskey(onSuccess, createPasskeyCallback)
                    }

                    is AuthResult.Failure -> {
                        var errorMessage: String? = null
                        val messageResId = when (val error = result.error) {
                            is AuthError.NetworkError -> R.string.error_network
                            is AuthError.UserAlreadyExists -> R.string.error_user_exists
                            is AuthError.InvalidCredentials -> R.string.error_invalid_credentials
                            is AuthError.ServerError -> {
                                errorMessage = error.message
                                R.string.error_server
                            }
                            is AuthError.Unknown -> {
                                errorMessage = error.message
                                R.string.error_unknown
                            }
                        }
                        _uiState.update {
                            it.copy(
                                messageResourceId = messageResId,
                                isLoading = false,
                                errorMessage = errorMessage
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    messageResourceId = R.string.enter_valid_username_and_display_name,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Creates a passkey. This is similar to the function in [CreatePasskeyViewModel].
     *
     * @param onSuccess Callback to be invoked when the passkey creation is successful.
     * @param createPasskey Reference to [CredentialManagerUtils.createPasskey]
     * The boolean parameter indicates whether the user should be navigated to the home screen.
     */
    private fun createPasskey(
        onSuccess: (navigateToHome: Boolean) -> Unit,
        createPasskey: suspend (JSONObject) -> GenericCredentialManagerResponse,
    ) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = repository.registerPasskeyCreationRequest()) {
                is AuthResult.Success -> {
                    val createPasskeyResponse = createPasskey(result.data)
                    if (createPasskeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                        when (repository.registerPasskeyCreationResponse(createPasskeyResponse.createPasskeyResponse)) {
                            is AuthResult.Success -> {
                                _uiState.update {
                                    it.copy(
                                        isSuccess = true,
                                        isLoading = false,
                                    )
                                }
                                onSuccess(false)
                            }

                            is AuthResult.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        isSuccess = false,
                                        isLoading = false,
                                        messageResourceId = R.string.some_error_occurred_please_check_logs
                                    )
                                }
                            }
                        }
                    } else if (createPasskeyResponse is GenericCredentialManagerResponse.Error) {
                        _uiState.update {
                            it.copy(
                                messageResourceId = R.string.some_error_occurred_please_check_logs,
                                errorMessage = createPasskeyResponse.errorMessage,
                                isLoading = false
                            )
                        }
                        repository.setSignedInState(false)
                    }
                }

                is AuthResult.Failure -> {
                    var errorMessage: String? = null
                    val messageResId = when (val error = result.error) {
                        is AuthError.NetworkError -> R.string.error_network
                        is AuthError.ServerError -> {
                            errorMessage = error.message
                            R.string.error_server
                        }
                        is AuthError.Unknown -> {
                            errorMessage = error.message
                            R.string.error_unknown
                        }
                        else -> R.string.error_unknown
                    }
                    _uiState.update {
                        it.copy(
                            messageResourceId = messageResId,
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                    onSuccess(false)
                }
            }
        }
    }

    /**
     * Registers a new user with a password.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @param onSuccess Lambda to be invoked when the registration is successful.
     * The boolean parameter indicates whether the user should be navigated to the home screen.
     * @param createPassword Lambda to be invoked when login is success and password needs to be saved
     */
    fun onPasswordRegister(
        username: String,
        password: String,
        onSuccess: (navigateToHome: Boolean) -> Unit,
        createPassword: suspend (String, String) -> Unit,
    ) {
        _uiState.update { it.copy(isLoading = true) }

        if (username.isNotEmpty() && password.isNotEmpty()) {
            viewModelScope.launch {
                when (val result = repository.login(username, password)) {
                    is AuthResult.Success -> {
                        createPassword(username, password)
                        _uiState.update {
                            it.copy(
                                isSuccess = true,
                                isLoading = false
                            )
                        }
                        onSuccess(true)
                    }

                    is AuthResult.Failure -> {
                        var errorMessage: String? = null
                        val messageResId = when (val error = result.error) {
                            is AuthError.NetworkError -> R.string.error_network
                            is AuthError.UserAlreadyExists -> R.string.error_user_exists
                            is AuthError.InvalidCredentials -> R.string.error_invalid_credentials
                            is AuthError.ServerError -> {
                                errorMessage = error.message
                                R.string.error_server
                            }
                            is AuthError.Unknown -> {
                                errorMessage = error.message
                                R.string.error_unknown
                            }
                        }
                        _uiState.update {
                            it.copy(
                                messageResourceId = messageResId,
                                isLoading = false,
                                errorMessage = errorMessage
                            )
                        }
                    }
                }
                repository.setSignedInState(false)
            }
        } else {
            _uiState.update {
                it.copy(
                    messageResourceId = R.string.enter_valid_username_and_password,
                    isLoading = false
                )
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
        viewModelScope.launch {
            when (val result = repository.registerPasskeyCreationRequest()) {
                is AuthResult.Success -> {
                    val createRestoreKeyResponse = createRestoreKeyOnCredMan(result.data)
                    if (createRestoreKeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                        repository.registerPasskeyCreationResponse(createRestoreKeyResponse.createPasskeyResponse)
                    }
                }

                is AuthResult.Failure -> {
                    // Don't block user registration if this fails.
                }
            }
        }
    }
}

/**
 * Represents the UI state for the registration screen.
 *
 * @param isLoading Indicates whether a registration operation is in progress.
 * @param isSuccess Indicates whether the registration was successful.
 * @param messageResourceId The resource ID of a message to display to the user.
 */
data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    @StringRes val messageResourceId: Int = R.string.empty_string,
    val errorMessage: String? = null,
)
