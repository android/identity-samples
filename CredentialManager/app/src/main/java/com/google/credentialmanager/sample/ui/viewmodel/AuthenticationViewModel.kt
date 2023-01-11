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

import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
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

    fun sendUsername(username: String) {
        viewModelScope.launch {
            val isSuccess = repository.sendUsername(username)
            if (isSuccess) {
                _uiState.update {
                    AuthUiState.MsgString(
                        "UserName verified successfully",
                        "", false
                    )
                }
            } else {
                _uiState.update {
                    AuthUiState.MsgString("Some error occurred, please check logs!", "", false)
                }
            }
        }
    }

        fun sendPassword(password: String) {
            viewModelScope.launch {
                val isSuccess = repository.password(password)
                if (isSuccess) {
                    _uiState.update {
                        AuthUiState.MsgString(
                            "Session-id stored successfully, Do register!",
                            "", false
                        )
                    }
                } else {
                    _uiState.update {
                        AuthUiState.MsgString("Some error occurred, please check logs!", "", false)
                    }
                }
            }
        }

        fun registerRequest() {
            viewModelScope.launch {
                repository.registerRequest()?.let { data ->
                    _uiState.update {
                        AuthUiState.CreationResult(data)
                    }
                }
            }
        }

        fun registerResponse(credential: CreatePublicKeyCredentialResponse) {
            viewModelScope.launch {
                val isRegisterResponseSuccess = repository.registerResponse(credential)
                if (isRegisterResponseSuccess) {
                    _uiState.update {
                        AuthUiState.MsgString(
                            "You have successfully created credentials. Now Sign in",
                            "register", true
                        )
                    }
                } else {
                    _uiState.update {
                        AuthUiState.MsgString(
                            "Some error occurred, please check logs!",
                            "register",
                            false
                        )
                    }
                }
            }
        }

        fun signInRequest() {
            viewModelScope.launch {
                val data = repository.signinRequest()
                data?.let { json ->
                    _uiState.update {
                        AuthUiState.RequestResult(json)
                    }
                }
            }
        }

        fun signInResponse(credential: GetCredentialResponse) {
            viewModelScope.launch {
                val isSuccess = repository.signinResponse(credential)
                if (isSuccess) {
                    _uiState.update {

                        AuthUiState.MsgString(
                            "Successfully Sign in, Navigating to Home",
                            "signin",
                            true
                        )
                    }
                } else {
                    _uiState.update {
                        AuthUiState.MsgString(
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

        class MsgString(

            val msg: String,
            val request: String,
            val success: Boolean
        ) : AuthUiState()

        class CreationResult(

            val data: JSONObject
        ) : AuthUiState()

        class RequestResult(

            val data: JSONObject
        ) : AuthUiState()
    }

