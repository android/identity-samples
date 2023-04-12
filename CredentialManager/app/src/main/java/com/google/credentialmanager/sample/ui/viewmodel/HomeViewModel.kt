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
class HomeViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Empty)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun registerRequest() {
        _uiState.update {
            HomeUiState.IsLoading
        }
        viewModelScope.launch {
            repository.registerRequest()?.let { data ->
                _uiState.update {
                    HomeUiState.CreationResult(data)
                }
            } ?: run {
                _uiState.update {
                    HomeUiState.MsgString(
                        "Oops, An internal server error occurred."
                    )
                }
            }
        }
    }

    fun registerResponse(credential: CreatePublicKeyCredentialResponse) {
        viewModelScope.launch {
            val isRegisterResponseSuccess = repository.registerResponse(credential)
            if (isRegisterResponseSuccess) {
                _uiState.update {
                    HomeUiState.MsgString(
                        "Passkey created. Try signin with passkeys"
                    )
                }
            } else {
                _uiState.update {
                    HomeUiState.MsgString(
                        "Some error occurred, please check logs!"
                    )
                }
            }
        }
    }
}

sealed class HomeUiState {
    object Empty : HomeUiState()

    object IsLoading : HomeUiState()

    class MsgString(

        val msg: String
    ) : HomeUiState()

    class CreationResult(

        val data: JSONObject
    ) : HomeUiState()
}

