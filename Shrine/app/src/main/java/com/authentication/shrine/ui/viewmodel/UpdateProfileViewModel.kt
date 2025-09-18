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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.repository.AuthRepository.Companion.CRED_ID
import com.authentication.shrine.repository.AuthRepository.Companion.RP_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.USER_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.read
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing the state and business logic for the user profile
 * update screen.
 *
 * @property credentialManagerUtils Utilities for interacting with the Credential Manager.
 * @property dataStore The DataStore instance used for reading persisted user and credential identifiers.
 */
@HiltViewModel
class UpdateProfileViewModel @Inject constructor(
    private val credentialManagerUtils: CredentialManagerUtils,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateProfileState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                UpdateProfileState(
                    userId = dataStore.read(USER_ID_KEY) ?: "",
                    rpId = dataStore.read(RP_ID_KEY) ?: "",
                    credentialId = dataStore.read(CRED_ID) ?: "",
                )
            }
        }
    }

    /**
     * Signals an update to the user's metadata (name and display name) through the
     * [CredentialManagerUtils].
     *
     * @param newName The new name for the user.
     * @param newDisplayName The new display name for the user.
     */
    fun updateMetadata(
        newName: String,
        newDisplayName: String,
    ) {
        viewModelScope.launch {
            credentialManagerUtils.signalUserDetails(newName, newDisplayName)
        }
    }
}

/**
 * Represents the state of the user profile update screen.
 *
 * @property userId The unique identifier for the user
 * @property rpId The identifier for the Relying Party
 * @property credentialId The identifier for the credential that needs to be updated
 */
data class UpdateProfileState(
    val userId: String = "",
    val rpId: String = "",
    val credentialId: String = "",
)
