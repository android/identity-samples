/*
 * Copyright 2025 Google LLC
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

package com.google.credentialmanager.sample

import android.app.Activity
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

/**
 * Creates a credential using the Credential Manager API.
 *
 * @param activity The activity to use for the create credential request.
 * @param request The create credential request.
 * @return The create credential response.
 */

suspend fun createCredential(
    activity: Activity,
    request: CreateCredentialRequest
): CreateCredentialResponse {
    val credentialManager = CredentialManager.create(activity)
    return credentialManager.createCredential(activity, request)
}

/**
 * Gets a credential using the Credential Manager API.
 *
 * @param activity The activity to use for the get credential request.
 * @param request The get credential request.
 * @return The get credential response.
 */
suspend fun getCredential(
    activity: Activity,
    request: GetCredentialRequest
): GetCredentialResponse {
    val credentialManager = CredentialManager.create(activity)
    return credentialManager.getCredential(activity, request)
}
