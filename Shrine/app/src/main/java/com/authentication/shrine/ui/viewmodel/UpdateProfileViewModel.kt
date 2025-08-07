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
import com.authentication.shrine.CredentialManagerUtils
import com.authentication.shrine.repository.AuthRepository.Companion.CRED_ID
import com.authentication.shrine.repository.AuthRepository.Companion.RP_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.USER_ID_KEY
import com.authentication.shrine.repository.AuthRepository.Companion.read
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateProfileViewModel @Inject constructor(
    private val credentialManagerUtils: CredentialManagerUtils,
    private val dataStore: DataStore<Preferences>,
    private val coroutineScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TestState())
    val uiState = _uiState.asStateFlow()

    init {
        coroutineScope.launch {
            _uiState.update {
                TestState(
                    userId = dataStore.read(USER_ID_KEY) ?: "",
                    rpId = dataStore.read(RP_ID_KEY) ?: "",
                    credentialId = dataStore.read(CRED_ID) ?: "",
                )
            }
        }
    }

    fun updateMetadata(
        newName: String,
        newDisplayName: String,
    ) {
        coroutineScope.launch {
            credentialManagerUtils.signalUserDetails(newName, newDisplayName)
        }
    }
}

data class TestState(
    val userId: String = "",
    val rpId: String = "",
    val credentialId: String = "",
)
