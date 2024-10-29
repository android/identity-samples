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
import com.authentication.shrine.model.AuthError
import com.authentication.shrine.model.AuthResult
import com.authentication.shrine.model.PasskeyCredential
import com.authentication.shrine.repository.AuthRepository
import com.authentication.shrine.repository.AuthRepository.Companion.RESTORE_CREDENTIAL_AAGUID
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
            val data = authRepository.getListOfPasskeys()
            if (data != null) {
                val filteredPasskeysList =
                    data.credentials.filter { passkey -> passkey.aaguid != RESTORE_CREDENTIAL_AAGUID }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userHasPasskeys = filteredPasskeysList.isNotEmpty(),
                        passkeysList = filteredPasskeysList,
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
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = authRepository.registerPasskeyCreationRequest()) {
                is AuthResult.Success -> {
                    val createPasskeyResponse = createPasskey(result.data)
                    if (createPasskeyResponse is GenericCredentialManagerResponse.CreatePasskeySuccess) {
                        when (authRepository.registerPasskeyCreationResponse(createPasskeyResponse.createPasskeyResponse)) {
                            is AuthResult.Success -> {
                                val passkeysList = authRepository.getListOfPasskeys()
                                if (passkeysList != null) {
                                    val filteredPasskeysList =
                                        passkeysList.credentials.filter { passkey -> passkey.aaguid != RESTORE_CREDENTIAL_AAGUID }
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            passkeysList = filteredPasskeysList,
                                            messageResourceId = R.string.passkey_created
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
                            }

                            is AuthResult.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        messageResourceId = R.string.some_error_occurred_please_check_logs
                                    )
                                }
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
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            when (val result = authRepository.deletePasskey(credentialId)) {
                is AuthResult.Success -> {
                    // Refresh passkeys list after deleting a passkey
                    val data = authRepository.getListOfPasskeys()
                    if (data != null) {
                        val filteredPasskeysList =
                            data.credentials.filter { passkey -> passkey.aaguid != RESTORE_CREDENTIAL_AAGUID }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userHasPasskeys = filteredPasskeysList.isNotEmpty(),
                                passkeysList = filteredPasskeysList,
                                messageResourceId = R.string.delete_passkey_successful
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
