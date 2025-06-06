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
import com.authentication.shrine.R
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

data class SettingsUiState(
    val isLoading: Boolean = false,
    val userHasPasskeys: Boolean = true,
    val username: String = "",
    val passkeysList: List<PasskeyCredential> = listOf(),
    val passwordChanged: String = "",
    @StringRes val errorMessage: Int = -1,
)
