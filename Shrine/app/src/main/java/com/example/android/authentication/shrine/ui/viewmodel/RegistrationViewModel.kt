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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.authentication.shrine.R
import com.example.android.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for user registration. Uses [AuthRepository] to interact with the authentication backend
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
     * Registers a new user.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @param onSuccess Lambda to be invoked when the registration is successful.
     * The boolean parameter indicates whether the user should be navigated to the home screen.
     * @param createPassword Lambda to be invoked when login is success and password needs to be saved
     */
    fun onRegister(
        username: String,
        password: String,
        onSuccess: (navigateToHome: Boolean) -> Unit,
        createPassword: suspend (String, String) -> Unit,
    ) {
        _uiState.value = RegisterUiState(isLoading = true)

        if (username.isNotEmpty() && password.isNotEmpty()) {
            viewModelScope.launch {
                val isSuccess = repository.login(username, password)
                if (isSuccess) {
                    createPassword(username, password)
                    _uiState.update {
                        RegisterUiState(
                            isSuccess = true,
                            messageResourceId = R.string.password_created_and_saved,
                        )
                    }
                    onSuccess(true)
                } else {
                    _uiState.update {
                        RegisterUiState(
                            messageResourceId = R.string.some_error_occurred_please_check_logs,
                        )
                    }
                }
                repository.setSignedInState(false)
            }
        } else {
            _uiState.update {
                RegisterUiState(
                    messageResourceId = R.string.enter_valid_username_and_password,
                )
            }
        }
    }
}

/**
 * Represents the UI state for the registration screen.
 *
 * @param isLoading Indicates whether a registration operation is in progress.
 * @param isSuccess Indicates whether the registration was successful.
 * @param successMessage A success message to display to the user, if any.
 * @param messageResourceId The resource ID of a message to display to the user.
 */
data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val successMessage: String = "",
    @StringRes val messageResourceId: Int = R.string.empty_string,
)
