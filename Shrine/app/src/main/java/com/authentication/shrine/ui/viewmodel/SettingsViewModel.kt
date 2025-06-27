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
 * ViewModel for the Settings screen.
 *
 * This ViewModel is responsible for managing the UI state of the settings screen,
 * including fetching user information and passkey details. It uses [AuthRepository]
 * to interact with the data layer.
 *
 * Annotated with {@link HiltViewModel} for Hilt dependency injection.
 *
 * @property authRepository The repository for authentication-related operations.
 * @see SettingsUiState
 * @see AuthRepository
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getPasskeysList()
        }
    }

    /**
     * Fetches the list of passkeys for the authenticated user and updates the UI state.
     *
     * This function updates {@link #_uiState} to indicate loading status,
     * and then asynchronously retrieves passkey data and username from {@link AuthRepository}.
     * On successful retrieval, it updates the state with the fetched data.
     * If retrieval fails, it updates the state with an error message.
     */
    private fun getPasskeysList() {
        _uiState.update {
            SettingsUiState(isLoading = true)
        }

        viewModelScope.launch {
            val data = authRepository.getListOfPasskeys()
            if (data != null) {
                _uiState.update {
                    SettingsUiState(
                        isLoading = false,
                        userHasPasskeys = data.credentials.isNotEmpty(),
                        username = authRepository.getUsername(),
                        passkeysList = data.credentials,
                    )
                }
            } else {
                _uiState.update {
                    SettingsUiState(
                        errorMessage = R.string.get_keys_error,
                    )
                }
            }
        }
    }
}

/**
 * Data class representing the UI state for the Settings screen.
 *
 * This class holds all the data necessary to render the settings UI,
 * including loading indicators, user information, passkey details, and error messages.
 *
 * @property isLoading True if data is currently being loaded, false otherwise.
 * @property userHasPasskeys True if the user has registered passkeys, false otherwise.
 *                     Defaults to true, assuming a user might have passkeys until checked.
 * @property username The display name of the authenticated user.
 * @property passkeysList A list of {@link PasskeyCredential} objects representing the user's passkeys.
 * @property passwordChanged A string indicating when the password was last changed (e.g., "2 days ago").
 *                         This property is not updated by the provided ViewModel snippet.
 * @property errorMessage A string resource ID for an error message to be displayed.
 *                      Defaults to -1, indicating no error.
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val userHasPasskeys: Boolean = true,
    val username: String = "",
    val passkeysList: List<PasskeyCredential> = listOf(),
    val passwordChanged: String = "",
    @StringRes val errorMessage: Int = -1,
)
