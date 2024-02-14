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
 * The PublicKeyCredentialRequestOptions dictionary of the Web Authentication API holds the options passed to client app's getCredential() call in order to fetch a given PublicKeyCredential.
 *
 * This class is used for demonstration purpose and we don't recommend you to use it directly on production.
 * Please refer to standard WebAuthn specs : https://www.w3.org/TR/webauthn-2/#dictionary-assertion-options
 */
class PublicKeyCredentialRequestOptions(requestJson: String) {
    private val json: JSONObject

    val challenge: ByteArray
    private val timeout: Long
    val rpId: String
    private val userVerification: String

    init {
        json = JSONObject(requestJson)

        val challengeString = json.getString("challenge")
        challenge = b64Decode(challengeString)
        timeout = json.optLong("timeout", 0)
        rpId = json.optString("rpId", "")
        userVerification = json.optString("userVerification", "preferred")
    }
}
