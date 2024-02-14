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
package com.example.android.authentication.myvault.ui.settings

import androidx.lifecycle.ViewModel
import com.example.android.authentication.myvault.data.room.MyVaultDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * This viewmodel holds the logic for deleting all the credentials saved on MyVault
 */
class SettingsViewModel(private val database: MyVaultDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Init)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Deletes all the data from the database.
     */
    fun deleteAllData() {
        database.clearAllTables()
        _uiState.update {
            UiState.Success
        }
    }

    /**
     * Represents the different states of the Settings screen.
     */
    sealed class UiState {

        /**
         * The initial state of the screen.
         */
        data object Init : UiState()

        /**
         * The state after the data has been deleted successfully.
         */
        data object Success : UiState()
    }
}
