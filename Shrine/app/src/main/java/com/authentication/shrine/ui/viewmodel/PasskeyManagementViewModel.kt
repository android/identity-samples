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
package com.authentication.shrine.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.R
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for user passkey and password registration. Uses [AuthRepository] to interact with the
 * authentication backend
 *
 * @param repository The authentication repository.
 */
@HiltViewModel
class PasskeyManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasskeyManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getPasskeysList()
        }
    }

    /**
     * Makes a request to get a list of passkeys from the server. Very similar to
     * [com.authentication.shrine.ui.viewmodel.SettingsViewModel.getPasskeysList].
     */
    fun getPasskeysList() {
        _uiState.update {
            PasskeyManagementUiState(isLoading = true)
        }

        viewModelScope.launch {
            try {
                val data = authRepository.getListOfPasskeys()
                if (data != null) {
                    _uiState.update {
                        PasskeyManagementUiState(
                            isLoading = false,
                            userHasPasskeys = data.credentials.isNotEmpty(),
                            passkeysList = data.credentials,
                        )
                    }
                } else {
                    _uiState.update {
                        PasskeyManagementUiState(
                            messageResourceId = R.string.get_keys_error,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    PasskeyManagementUiState(
                        errorMessage = e.message,
                    )
                }
            }
        }
    }

    /**
     * Makes a request to delete a passkey from the server. Refreshes the passkey list upon
     * successful deletion.
     *
     * @param credentialId The ID of the passkey to delete.
     */
    fun deletePasskey(credentialId: String) {
        _uiState.update {
            PasskeyManagementUiState(isLoading = true)
        }

        viewModelScope.launch {
            try {
                if (authRepository.deletePasskey(credentialId)) {
                    // Refresh passkeys list after deleting a passkey
                    getPasskeysList()
                    _uiState.update {
                        PasskeyManagementUiState(
                            deleteStatus = R.string.delete_passkey_successful,
                        )
                    }
                } else {
                    _uiState.update {
                        PasskeyManagementUiState(
                            deleteStatus = R.string.delete_passkey_error,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    PasskeyManagementUiState(
                        errorMessage = e.message,
                    )
                }
            }
        }
    }
}

/**
 * Represents the UI state for the passkey management screen.
 *
 * @param isLoading Indicates whether a modification operation is in progress.
 * @param userHasPasskeys Indicates whether the user has passkeys.
 * @param passkeysList A list of passkeys for the user returned from the server.
 * @param messageResourceId The resource ID of a message to display to the user.
 * @param errorMessage An error message returned from the server.
 * @param deleteStatus The resource ID of a message to display to the user on successful passkey
 * deletion.
 */
data class PasskeyManagementUiState(
    val isLoading: Boolean = false,
    val userHasPasskeys: Boolean = true,
    val passkeysList: List<PasskeyCredential> = listOf(),
    @StringRes val messageResourceId: Int = R.string.empty_string,
    val errorMessage: String? = null,
    @StringRes val deleteStatus: Int = R.string.empty_string,
)