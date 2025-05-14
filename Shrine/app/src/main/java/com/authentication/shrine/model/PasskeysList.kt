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
package com.authentication.shrine.model

/**
 * Data class for fetching list of passkeys from the /getKeys endpoint
 *
 * @param rpId Relying Party ID
 * @param userId User ID
 * @param credentials List of credentials data
 * */
data class PasskeysList(
    var rpId: String,
    var userId: String,
    val credentials: List<PasskeyCredential>,
)

/**
 * Data class for holding credential data
 *
 * @param id Credential ID
 * @param passkeyUserId UserId corresponding to the passkey
 * @param name Name of the credential Provider
 * @param credentialType Type of the credential
 * @param aaguid AAGUID corresponding to the passkey
 * @param registeredAt Time of creation of the passkey
 * @param providerIcon Icon for the credential provider
 * */
data class PasskeyCredential(
    val id: String,
    val passkeyUserId: String,
    val name: String,
    val credentialType: String,
    val aaguid: String,
    val registeredAt: Long,
    val providerIcon: String,
)
