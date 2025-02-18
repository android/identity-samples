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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.authentication.shrine.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A ViewModel that handles home screen-related operations.
 *
 * This ViewModel is responsible for signing out the user.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    /**
     * Signs out the user.
     *
     * @param deleteRestoreKey Lambda function received from Composable that triggers
     * Credential Manager's deleteRestoreKey
     */
    fun signOut(
        deleteRestoreKey: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            repository.signOut()
            deleteRestoreKey()
        }
    }
}
