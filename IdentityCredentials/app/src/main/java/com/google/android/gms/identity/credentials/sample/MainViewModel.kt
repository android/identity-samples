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
package com.google.android.gms.identity.credentials.sample

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.identitycredentials.ClearRegistryResponse
import com.google.android.gms.identitycredentials.Credential
import com.google.android.gms.identitycredentials.CredentialOption
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle
import com.google.android.gms.identitycredentials.RegistrationResponse
import com.google.android.gms.tasks.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class State(
    val credential: Credential? = null
)

val State.hasCredential get() = credential != null

/** ViewModel is used to access the Identity Credential Data and to observe changes to it. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: IdentityCredentialsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Log.d("", "init")
        }
    }

    fun registerCredentials(
        credentials: ByteArray, matcher: ByteArray, type: String,
        requestType: String, protocolTypes: List<String>, id: String
    ) {
        viewModelScope.launch {
            var response: RegistrationResponse = repository.registerCredentials(credentials, matcher, type, requestType, protocolTypes, id).await()
            // _state.value = State(credential = result)
        }
    }

    /** Launch the credential-selector UI. */
    fun launchCredentialSelector(
        credentialOptions: List<CredentialOption>, data: Bundle,
        origin: String?, resultReceiver: ResultReceiver
    ) {
        viewModelScope.launch {
            val intent: PendingIntent = getCredentialSelectorPendingIntent(
                credentialOptions, data, origin, resultReceiver
            )
            try {
                intent.send()
            } catch (e: CanceledException) {
                e.printStackTrace()
            }
        }
    }

    /** Obtain the PendingIntent to launch the credential-selector UI. */
    private suspend fun getCredentialSelectorPendingIntent(
        credentialOptions: List<CredentialOption>, data: Bundle,
        origin: String?, resultReceiver: ResultReceiver
    ): PendingIntent {
        val response: Task<PendingGetCredentialHandle> = repository.getCredential(credentialOptions, data, origin, resultReceiver)
        response.await()
        return response.result.pendingIntent
    }

     fun clearRegistry() {
        viewModelScope.launch {
            var response: ClearRegistryResponse = repository.clearRegistry().await()
            _state.value = State(credential =  null)
        }
    }
}
