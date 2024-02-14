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
import java.security.MessageDigest

/**
 * The AuthenticatorAssertionResponse interface of the Web Authentication API contains a digital signature from the private key of a particular WebAuthn credential.
 *
 * The relying party's server can verify this signature to authenticate a user, for example when they sign in.
 *
 * This class is used for demonstration purpose and we don't recommend you to use it directly on production.
 * Please refer to standard WebAuthn specs for AuthenticatorAssertionResponse : https://www.w3.org/TR/webauthn-2/#iface-authenticatorassertionresponse
 */
class AuthenticatorAssertionResponse(
    private val requestOptions: PublicKeyCredentialRequestOptions,
    origin: String,
    private val up: Boolean,
    private val uv: Boolean,
    private val be: Boolean,
    private val bs: Boolean,
    private var userHandle: ByteArray,
    packageName: String? = null,
    private val clientDataHash: ByteArray? = null,
) : AuthenticatorResponse {
    override var clientJson = JSONObject()
    private var authenticatorData: ByteArray
    var signature: ByteArray = byteArrayOf()

    init {
        clientJson.put("type", "webauthn.get")
        clientJson.put("challenge", b64Encode(requestOptions.challenge))
        clientJson.put("origin", origin)
        if (packageName != null) {
            clientJson.put("androidPackageName", packageName)
        }

        authenticatorData = defaultAuthenticatorData()
    }

    /**
     * Generates the default authenticator data.
     *
     * @return The default authenticator data.
     */
    private fun defaultAuthenticatorData(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val rpHash = md.digest(requestOptions.rpId.toByteArray())
        var flags = 0
        if (up) {
            flags = flags or 0x01
        }
        if (uv) {
            flags = flags or 0x04
        }
        if (be) {
            flags = flags or 0x08
        }
        if (bs) {
            flags = flags or 0x10
        }
        return rpHash +
            byteArrayOf(flags.toByte()) +
            byteArrayOf(0, 0, 0, 0)
    }

    /**
     * Computes the data to sign.
     *
     * @return The data to sign.
     */
    fun dataToSign(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val hash: ByteArray = clientDataHash ?: md.digest(clientJson.toString().toByteArray())

        return authenticatorData + hash
    }

    /**
     * Converts the response to a JSON object.
     *
     * @return The JSON object representation of the response.
     */
    override fun json(): JSONObject {
        val clientData = clientJson.toString().toByteArray()
        val response = JSONObject()
        if (clientDataHash == null) {
            response.put("clientDataJSON", b64Encode(clientData))
        }
        response.put("authenticatorData", b64Encode(authenticatorData))
        response.put("signature", b64Encode(signature))
        response.put("userHandle", b64Encode(userHandle))
        return response
    }
}
