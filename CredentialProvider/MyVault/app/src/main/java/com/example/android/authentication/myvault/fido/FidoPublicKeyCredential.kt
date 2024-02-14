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
 * The PublicKeyCredential interface inherits from Credential,  and contains the attributes that are returned to the caller when a new credential is created, or a new assertion is requested.
 *
 * This class is used for demonstration purpose and we don't recommend you to use it directly on production.
 * Please refer to standard WebAuthn specs : https://www.w3.org/TR/webauthn-2/#iface-pkcredential
 */
class FidoPublicKeyCredential(
    val rawId: ByteArray,
    val response: AuthenticatorResponse,
    val authenticatorAttachment: String,
) {

    fun json(): String {
        // See RegistrationResponseJSON at
        // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson
        // https://www.w3.org/TR/webauthn-2/#publickeycredential

        val encodedId = b64Encode(rawId)
        val ret = JSONObject()
        ret.put("id", encodedId)
        ret.put("rawId", encodedId)
        ret.put("type", "public-key")
        ret.put("authenticatorAttachment", authenticatorAttachment)
        ret.put("response", response.json())
        ret.put("clientExtensionResults", extensionJson())
        return ret.toString()
    }

    private fun extensionJson(): JSONObject {
        val json = JSONObject()
        json.put("credProps", credPropsJson())
        return json
    }

    private fun credPropsJson(): JSONObject {
        val response = JSONObject()
        response.put("rk", true)
        return response
    }
}
