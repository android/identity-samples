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
package com.authentication.shrine.model

/**
 * Represents the request body for completing a WebAuthn registration ceremony.
 * This data is sent to the server after the client (e.g., browser or app)
 * has successfully created a new passkey using `navigator.credentials.create()`.
 *
 * It contains the public key credential and related attestation data.
 *
 * @property id The base64url-encoded credential ID of the newly created passkey.
 *              This is the identifier for the credential.
 * @property type The type of the credential. Typically "public-key".
 * @property rawId The raw ID of the newly created passkey, in base64url encoding.
 *                 This is the byte array version of the `id`.
 * @property response The client data and attestation object, which are part of the
 *                    `PublicKeyCredential` returned by the WebAuthn API.
 * @see CredmanResponse
 */
data class RegisterResponseRequestBody(
    val id: String,
    val type: String,
    val rawId: String,
    val response: CredmanResponse,
)

/**
 * Represents the `response` part of a `PublicKeyCredential` obtained during a
 * WebAuthn registration ceremony. It contains the client data and the attestation object.
 *
 * This structure is typically nested within {@link RegisterResponseRequestBody}.
 *
 * @property clientDataJSON A JSON string containing client data about the registration ceremony,
 *                          such as the challenge, origin, and type of operation.
 *                          This data is signed by the authenticator.
 * @property attestationObject A base64url-encoded CBOR object containing the attestation statement.
 *                             This object provides information about the authenticator and the
 *                             newly created public key credential, allowing the Relying Party
 *                             to verify the authenticity and properties of the credential.
 */
data class CredmanResponse(
    val clientDataJSON: String,
    val attestationObject: String,
)
