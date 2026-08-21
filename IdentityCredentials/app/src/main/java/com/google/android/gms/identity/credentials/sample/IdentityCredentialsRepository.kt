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

import android.os.Bundle
import android.os.ResultReceiver
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.ClearRegistryResponse
import com.google.android.gms.identitycredentials.CredentialOption
import com.google.android.gms.identitycredentials.GetCredentialRequest
import com.google.android.gms.identitycredentials.IdentityCredentialClient
import com.google.android.gms.identitycredentials.PendingGetCredentialHandle
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.google.android.gms.identitycredentials.RegistrationResponse
import com.google.android.gms.tasks.Task
import javax.inject.Inject

/**
 * The Repository handles data operations and provides a clean API so that
 * the rest of the app can retrieve the Identity Credential data easily.
 * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/identitycredentials/IdentityCredentialClient">IdentityCredentialClient</a>
 * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes">CommonStatusCodes</a>
 */
class IdentityCredentialsRepository @Inject constructor(
    private val client: IdentityCredentialClient
) {
    /**
     * Returns a Task which asynchronously generates a RegistrationResponse on success
     * or throws an OperationException on failure, when attempting to write to the registry.
     *
     * eg.  com.google.android.gms.common.api.ApiException: 17 means:
     * The client attempted to call a method from an API that failed to connect.
     */
    fun registerCredentials(
        credentials: ByteArray, matcher: ByteArray, type: String,
        requestType: String, protocolTypes: List<String>, id: String
    ): Task<RegistrationResponse> {
        return client.registerCredentials(
            RegistrationRequest(credentials, matcher, type, requestType, protocolTypes, id)
        )
    }

    /** Returns a Task which asynchronously generates a pending intent to get credentials. */
    fun getCredential(
        credentialOptions: List<CredentialOption>, data: Bundle,
        origin: String?, resultReceiver: ResultReceiver
    ): Task<PendingGetCredentialHandle> {
        return client.getCredential(
            GetCredentialRequest(credentialOptions, data, origin, resultReceiver)
        )
    }

    /**
     * Returns a Task which asynchronously generates a ClearRegistryResponse on success
     * or throws an OperationException on failure, when attempting to clear from the registry.
     */
    fun clearRegistry(): Task<ClearRegistryResponse> {
        return client.clearRegistry(
            ClearRegistryRequest()
        )
    }
}
