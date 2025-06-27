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
 * Represents the server's response to a WebAuthn sign-in request.
 * This data contains the parameters needed by the client (e.g., a browser or app)
 * to call `navigator.credentials.get()`. It specifies the challenge, allowed credentials,
 * timeout, and user verification requirements for the sign-in ceremony.
 *
 * @property challenge A server-generated cryptographic challenge that must be signed by the authenticator.
 *                     This prevents replay attacks.
 * @property allowCredentials A list of credentials that are allowed to be used for sign-in.
 *                            Each {@link CredentialDetail} typically contains the `id` and `type`
 *                            of a credential previously registered by the user. If empty, the client
 *                            may allow the user to choose from any available passkey for the `rpId`.
 * @property timeout The time, in milliseconds, that the client has to complete the sign-in ceremony.
 * @property userVerification Specifies the Relying Party's user verification requirement for this
 *                            authentication ceremony (e.g., "required", "preferred", "discouraged").
 * @property rpId The effective domain of the Relying Party (the server or service) for which
 *                the assertion is being requested. This should match the `rpId` used during registration.
 * @see CredentialDetail
 */
data class SignInRequestResponse(
    val challenge: String,
    val allowCredentials: List<CredentialDetail>,
    val timeout: Long,
    val userVerification: String,
    val rpId: String,
)
