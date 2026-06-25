/*
 * Copyright 2026 Google LLC
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

package com.example.digitalcredentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Initial)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Orchestrates a Digital Credential (e.g., mDL) request.
     *
     * @param requestBlock A suspend lambda that performs the retrieval and returns a [MainUiState].
     */
    fun getDigitalCredential(requestBlock: suspend () -> MainUiState) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            _uiState.value = requestBlock()
        }
    }

    /**
     * Orchestrates a Verified Email credential request.
     *
     * @param requestBlock A suspend lambda that performs the retrieval and returns a [MainUiState].
     */
    fun getVerifiedEmailCredential(requestBlock: suspend () -> MainUiState) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            _uiState.value = requestBlock()
        }
    }
}
