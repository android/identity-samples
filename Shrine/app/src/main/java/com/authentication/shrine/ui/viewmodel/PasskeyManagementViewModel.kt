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

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.GenericCredentialManagerResponse
import com.authentication.shrine.R
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * ViewModel for user passkey and password registration. Uses [AuthRepository] to interact with the
 * authentication backend
 *
 * @param authRepository The authentication repository.
 * @param application The application.
 */
@HiltViewModel
class PasskeyManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val application: Application
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasskeyManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadAaguidData()
            getPasskeysList()
        }
    }

    private fun loadAaguidData() {
        // Use viewModelScope for coroutine
        viewModelScope.launch(Dispatchers.IO) { // Use IO dispatcher for file reading
            try {
                val gson = Gson()
                val aaguidInputStream = application.assets.open("aaguids.json")
                val reader = InputStreamReader(aaguidInputStream)
                val aaguidJsonData = gson.fromJson<Map<String, Map<String, String>>>(
                    reader,
                    object : TypeToken<Map<String, Map<String, String>>>() {}.type
                )
                _uiState.update { it.copy(aaguidData = aaguidJsonData) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messageResourceId = R.string.get_aaguid_error,
                    )
                }
            }
        }
    }

    /**
     * Makes a request to get a list of passkeys from the server. Very similar to
     * [com.authentication.shrine.ui.viewmodel.SettingsViewModel.getPasskeysList].
     */
    fun getPasskeysList() {
        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                val data = authRepository.getListOfPasskeys()
                if (data != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userHasPasskeys = data.credentials.isNotEmpty(),
                            passkeysList = data.credentials,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messageResourceId = R.string.get_keys_error,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message,
                    )
                }
            }
        }
    }

    /**
     * Creates a passkey. This is similar to the function in [CreatePasskeyViewModel].
     *
     * @param createPasskey Reference to [CredentialManagerUtils.createPasskey]
     */
    fun createPasskey(
        createPasskey: suspend (JSONObject) -> GenericCredentialManagerResponse,
    ) {
        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                val data = authRepository.registerPasskeyCreationRequest()
                if (data != null) {
                    val createPasskeyResponse = createPasskey(data)
                    if (createPasskeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                        val isRegisterResponse =
                            authRepository.registerPasskeyCreationResponse(createPasskeyResponse.createPasskeyResponse)
                        if (isRegisterResponse) {
                            val passkeysList = authRepository.getListOfPasskeys()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    passkeysList = passkeysList?.credentials ?: emptyList(),
                                    messageResourceId = R.string.passkey_created
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    messageResourceId = R.string.some_error_occurred_please_check_logs
                                )
                            }
                        }
                    } else if (createPasskeyResponse is GenericCredentialManagerResponse.Error) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = createPasskeyResponse.errorMessage
                            )
                        }
                        authRepository.setSignedInState(false)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messageResourceId = R.string.oops_an_internal_server_error_occurred
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                authRepository.setSignedInState(false)
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
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                if (authRepository.deletePasskey(credentialId)) {
                    // Refresh passkeys list after deleting a passkey
                    val data = authRepository.getListOfPasskeys()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userHasPasskeys = data?.credentials?.isNotEmpty() ?: false,
                            passkeysList = data?.credentials ?: emptyList(),
                            messageResourceId = R.string.delete_passkey_successful
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messageResourceId = R.string.delete_passkey_error,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
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
 */
data class PasskeyManagementUiState(
    val aaguidData: Map<String, Map<String, String>> = emptyMap(),
    val isLoading: Boolean = false,
    val userHasPasskeys: Boolean = true,
    val passkeysList: List<PasskeyCredential> = listOf(),
    @StringRes val messageResourceId: Int = R.string.empty_string,
    val errorMessage: String? = null,
)