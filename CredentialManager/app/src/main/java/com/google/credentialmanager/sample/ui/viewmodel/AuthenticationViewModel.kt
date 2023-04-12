/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.credentialmanager.sample.ui.viewmodel

import androidx.credentials.Credential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.credentialmanager.sample.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(private val repository: AuthRepository) :
    ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Empty)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        _uiState.update { AuthUiState.IsLoading }
        viewModelScope.launch {
            val isSuccess = repository.login(username, password)
            if (isSuccess) {
                _uiState.update {
                    AuthUiState.LoginResult(true, "Sign in successfully")
                }
            } else {
                _uiState.update {
                    AuthUiState.MsgString(
                        null,
                        "Some error occurred, please check logs!",
                        "",
                        false
                    )
                }
            }
            repository.setSignedInState(false)
        }
    }

    fun signInRequest() {
        _uiState.update { AuthUiState.IsLoading }
        viewModelScope.launch {
            val data = repository.signinRequest()
            data?.let { json ->
                _uiState.update {
                    AuthUiState.RequestResult(json)
                }
            }
        }
    }

    fun signInResponse(response: GetCredentialResponse) {
        viewModelScope.launch {
            val isSuccess = repository.signinResponse(response)
            if (isSuccess) {

                if (response.credential is PasswordCredential) {
                    repository.setSignedInState(false)
                } else {
                    repository.setSignedInState(true)
                }

                _uiState.update {
                    AuthUiState.MsgString(
                        response.credential,
                        "Successfully Sign in, Navigating to Home",
                        "signin",
                        true
                    )
                }
            } else {
                repository.setSignedInState(false)
                _uiState.update {
                    AuthUiState.MsgString(
                        null,
                        "Some error occurred, please check logs!",
                        "signin",
                        false
                    )
                }
            }
        }
    }
}

sealed class AuthUiState {

    object Empty : AuthUiState()

    object IsLoading : AuthUiState()

    class MsgString(

        val credential: Credential?,
        val msg: String,
        val request: String,
        val success: Boolean
    ) : AuthUiState()

    class LoginResult(

        val flag: Boolean,
        val msg: String
    ) : AuthUiState()

    class RequestResult(

        val data: JSONObject
    ) : AuthUiState()
}

