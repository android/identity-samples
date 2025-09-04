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
 * ViewModel for creating passkeys. Uses [AuthRepository] to interact with the authentication backend
 * and [CredentialManagerUtils] to interact with the Credential Manager API.
 *
 * @param repository The authentication repository.
 * @param credentialManagerUtils The Credential Manager utility.
 */
@HiltViewModel
class CreatePasskeyViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val credentialManagerUtils: CredentialManagerUtils,
) : ViewModel() {

    /**
     * UI state for the create passkey screen.
     */
    private val _uiState = MutableStateFlow(CreatePasskeyUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Creates a passkey.
     *
     * @param onSuccess Callback to be invoked when the passkey creation is successful.
     * @param createPasskey Reference to [CredentialManagerUtils.createPasskey]
     * The boolean parameter indicates whether the user should be navigated to the home screen.
     */
    fun createPasskey(
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
                                        navigateToMainMenu = true
                                    )
                                }
                                onSuccess(true)
                            }

                            is AuthResult.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        navigateToMainMenu = true,
                                        messageResourceId = R.string.some_error_occurred_please_check_logs
                                    )
                                }
                                onSuccess(true)
                            }
                        }
                    } else if (createPasskeyResponse is GenericCredentialManagerResponse.Error) {
                        _uiState.update {
                            it.copy(
                                errorMessage = createPasskeyResponse.errorMessage,
                                isLoading = false
                            )
                        }
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
}

/**
 * Represents the UI state for the create passkey screen.
 *
 * @param isLoading Indicates whether a passkey creation operation is in progress.
 * @param navigateToMainMenu Indicates whether to navigate to the main menu screen.
 * @param messageResourceId The resource ID of a message to display to the user.
 * @param errorMessage An error message to display to the user, if any.
 */
data class CreatePasskeyUiState(
    val isLoading: Boolean = false,
    val navigateToMainMenu: Boolean = false,
    @StringRes val messageResourceId: Int? = null,
    val errorMessage: String? = null,
)
