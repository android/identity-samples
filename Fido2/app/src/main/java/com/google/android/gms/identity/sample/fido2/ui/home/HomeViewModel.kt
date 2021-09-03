/*
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.identity.sample.fido2.ui.home

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.identity.sample.fido2.repository.AuthRepository
import com.google.android.gms.identity.sample.fido2.repository.SignInState
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _processing = MutableStateFlow(false)
    val processing = _processing.asStateFlow()

    val currentUsername = repository.signInState.map { state ->
        when (state) {
            is SignInState.SigningIn -> state.username
            is SignInState.SignedIn -> state.username
            else -> "(user)"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "(user)")

    val credentials = repository.credentials.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), emptyList()
    )

    fun reauth() {
        viewModelScope.launch {
            repository.clearCredentials()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    suspend fun registerRequest(): PendingIntent? {
        _processing.value = true
        try {
            return repository.registerRequest()
        } finally {
            _processing.value = false
        }
    }

    fun registerResponse(credential: PublicKeyCredential) {
        viewModelScope.launch {
            _processing.value = true
            try {
                repository.registerResponse(credential)
            } finally {
                _processing.value = false
            }
        }
    }

    fun removeKey(credentialId: String) {
        viewModelScope.launch {
            _processing.value = true
            try {
                repository.removeKey(credentialId)
            } finally {
                _processing.value = false
            }
        }
    }

}
