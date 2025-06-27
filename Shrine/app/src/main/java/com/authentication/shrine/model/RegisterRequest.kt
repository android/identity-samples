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
 * Represents the request body for initiating a WebAuthn registration ceremony.
 * This data is sent to the server to request options for creating a new passkey.
 *
 * @property attestation The desired attestation conveyance preference. Defaults to "none".
 *                       Common values include "none", "indirect", "direct".
 * @property authenticatorSelection Specifies requirements for the authenticator to be used.
 *                                  Defaults to a standard {@link AuthenticatorSelection} configuration.
 * @see AuthenticatorSelection
 */
data class RegisterRequestRequestBody(
    val attestation: String = "none",
    val authenticatorSelection: AuthenticatorSelection = AuthenticatorSelection(),
)

/**
 * Represents the server's response when requesting WebAuthn registration options.
 * This data contains the parameters needed by the client (e.g., a browser or app)
 * to call `navigator.credentials.create()`.
 *
 * @property authenticatorSelection Specifies authenticator selection criteria required by the server.
 * @property challenge A server-generated cryptographic challenge that must be signed by the authenticator.
 * @property excludeCredentials A list of credentials that should not be created again for this user,
 *                              used to prevent duplicate registrations.
 * @property pubKeyCredParams A list of public key credential types and algorithms supported by the Relying Party (server).
 * @property rp Information about the Relying Party (the server or service).
 * @property timeout The time, in milliseconds, that the client has to complete the registration ceremony.
 * @property user Information about the user for whom the credential is being registered.
 * @see AuthenticatorSelection
 * @see CredentialDetail
 * @see PubKeyCredParam
 * @see Rp
 * @see User
 */
data class RegisterRequestResponse(
    val authenticatorSelection: AuthenticatorSelection,
    val challenge: String,
    val excludeCredentials: List<CredentialDetail>,
    val pubKeyCredParams: List<PubKeyCredParam>,
    val rp: Rp,
    val timeout: Int,
    val user: User,
)

/**
 * Specifies requirements for the authenticator during a WebAuthn ceremony (registration or authentication).
 *
 * @property authenticatorAttachment Specifies the desired authenticator attachment modality.
 *                                   Defaults to "platform" (e.g., built-in sensor like Touch ID or Windows Hello).
 *                                   Other common value is "cross-platform" (e.g., a USB security key).
 * @property userVerification Specifies the Relying Party's user verification requirement.
 *                            Defaults to "required" (e.g., user must verify with PIN, biometric).
 *                            Other values: "preferred", "discouraged".
 * @property requireResidentKey Indicates if the authenticator should create a client-side discoverable credential (resident key).
 *                              Defaults to true.
 * @property residentKey An alternative way to specify the resident key requirement, often aligned with `requireResidentKey`.
 *                       Defaults to "required".
 */
data class AuthenticatorSelection(
    val authenticatorAttachment: String = "platform",
    val userVerification: String = "required",
    val requireResidentKey: Boolean = true,
    val residentKey: String = "required",
)

/**
 * Describes a public key credential type and the cryptographic algorithm it uses.
 * Part of the `pubKeyCredParams` in {@link RegisterRequestResponse}.
 *
 * @property alg The COSE algorithm identifier for the public key. (e.g., -7 for ES256, -257 for RS256).
 * @property type The type of the public key credential. Typically "public-key".
 */
data class PubKeyCredParam(
    val alg: Int,
    val type: String,
)

/**
 * Represents the Relying Party (RP) in a WebAuthn ceremony.
 * The RP is the server or service that the user is trying to register with or authenticate to.
 *
 * @property id The effective domain of the Relying Party. This MUST be a valid domain string.
 * @property name A human-readable name for the Relying Party.
 */
data class Rp(
    val id: String,
    val name: String,
)

/**
 * Represents the user for whom a WebAuthn credential is being registered or asserted.
 *
 * @property displayName A human-readable name for the user account, chosen by the user.
 * @property id A unique, server-chosen identifier for the user account (e.g., a UUID or username).
 *              This should not be personally identifiable information if possible.
 * @property name A human-readable name for the user account, which may be the same as `displayName` or a username.
 *                The distinction can vary based on RP implementation.
 */
data class User(
    val displayName: String,
    val id: String,
    val name: String,
)

/**
 * Describes an existing WebAuthn credential, often used in `excludeCredentials`
 * to prevent re-registration of an already existing passkey.
 *
 * @property id The base64url-encoded credential ID of an existing public key credential.
 * @property type The type of the credential. Typically "public-key".
 */
data class CredentialDetail(
    val id: String,
    val type: String,
)
