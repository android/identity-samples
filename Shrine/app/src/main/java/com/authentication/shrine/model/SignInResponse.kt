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
 * Represents the request body for completing a WebAuthn sign-in ceremony.
 * This data is sent to the server after the client (e.g., browser or app)
 * has successfully retrieved a passkey using `navigator.credentials.get()`. It contains the
 * assertion and related data.
 *
 * @property id The base64url-encoded credential ID of the passkey used for sign-in.
 * @property type The type of the credential. Typically "public-key".
 * @property rawId The raw ID of the passkey used for sign-in, in base64url encoding.
 *                 This is the byte array version of the `id`.
 * @property response The client data, authenticator data, signature, and user handle, which are
 *                    part of the `PublicKeyCredential` returned by the WebAuthn API.
 * @see ResponseObject
 */
data class SignInResponseRequest(
    val id: String,
    val type: String,
    val rawId: String,
    val response: ResponseObject,
)

/**
 * Represents the `response` part of a `PublicKeyCredential` obtained during a
 * WebAuthn sign-in ceremony. It contains the client data, authenticator data, signature, and user handle.
 *
 * This structure is typically nested within {@link SignInResponseRequest}.
 *
 * @property clientDataJSON A JSON string containing client data about the sign-in ceremony, such as
 *                          the challenge, origin, and type of operation.
 *                          This data is signed by the authenticator.
 * @property authenticatorData A base64url-encoded CBOR object containing the authenticator data.
 *                             This object provides information about the authenticator and the
 *                             assertion, allowing the Relying Party to verify the authenticity
 *                             and properties of the assertion.
 * @property signature A base64url-encoded signature over the `clientDataJSON` and `authenticatorData`
 *                       using the private key of the passkey.
 * @property userHandle An optional base64url-encoded user handle, which can be used to identify
 *                       the user within the Relying Party's system. This may or may not be present
 *                       depending on whether a resident key was used and how the server requested it.
 */
data class ResponseObject(
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String,
)
