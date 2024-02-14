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
package com.example.android.authentication.myvault.fido

import org.json.JSONObject

/**
 * The AuthenticatorResponse interface of the Web Authentication API is the result of a WebAuthn credential registration.
 * It contains information about the credential that the server needs to perform WebAuthn assertions, such as its credential ID and public key.
 *
 * This class is used for demonstration purpose and we don't recommend you to use it directly on production.
 * Please refer to standard WebAuthn specs for AuthenticatorResponse : https://www.w3.org/TR/webauthn-2/#authenticatorresponse
 */
interface AuthenticatorResponse {
    var clientJson: JSONObject
    fun json(): JSONObject
}
