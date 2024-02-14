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

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/*
* The AuthenticatorAttestationResponse interface of the Web Authentication API is the result of a WebAuthn credential registration.
* It contains information about the credential that the server needs to perform WebAuthn assertions, such as its credential ID and public key.
*
* This class is used for demonstration purpose and we don't recommend you to use it directly on production.
* Please refer to standard WebAuthn specs for AuthenticatorAttestationResponse : https://www.w3.org/TR/webauthn-2/#authenticatorattestationresponse
 */
class AuthenticatorAttestationResponse(
    private val requestOptions: PublicKeyCredentialCreationOptions,
    private val credentialId: ByteArray,
    private val credentialPublicKey: ByteArray,
    origin: String,
    private val up: Boolean,
    private val uv: Boolean,
    private val be: Boolean,
    private val bs: Boolean,
    packageName: String? = null,
    private val clientDataHash: ByteArray? = null,
    private val spki: ByteArray? = null,
) : AuthenticatorResponse {
    override var clientJson = JSONObject()
    private var attestationObject: ByteArray

    init {
        clientJson.put("type", "webauthn.create")
        clientJson.put("challenge", b64Encode(requestOptions.challenge))
        clientJson.put("origin", origin)
        if (packageName != null) {
            clientJson.put("androidPackageName", packageName)
        }

        attestationObject = defaultAttestationObject()
    }

    private fun authData(): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val rpHash = md.digest(requestOptions.rp.id.toByteArray())
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
        flags = flags or 0x40

        val aaguid = ByteArray(16) { 0 }
        val credIdLen = byteArrayOf((credentialId.size shr 8).toByte(), credentialId.size.toByte())

        return rpHash +
            byteArrayOf(flags.toByte()) +
            byteArrayOf(0, 0, 0, 0) +
            aaguid +
            credIdLen +
            credentialId +
            credentialPublicKey
    }

    private fun addParsedAttestationObjectFieldsToJSON(
        authData: ByteArray,
        publicKeyAlgorithm: Long,
        jsonOutput: JSONObject,
    ) {
        // https://www.w3.org/TR/webauthn-2/#sctn-generating-an-attestation-object
        jsonOutput.put(
            "authenticatorData",
            b64Encode(authData),
        )
        jsonOutput.put("publicKeyAlgorithm", publicKeyAlgorithm)
        if (spki != null) {
            jsonOutput.put("publicKey", b64Encode(spki))
        } else {
            Log.i("AuthAttest", " Public key is null")
        }
    }

    private fun defaultAttestationObject(): ByteArray {
        val ao = mutableMapOf<String, Any>()
        ao["fmt"] = "none"
        ao["attStmt"] = emptyMap<Any, Any>()
        ao["authData"] = authData()
        return Cbor().encode(ao)
    }

    override fun json(): JSONObject {
        // See AuthenticatorAttestationResponseJSON at
        // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson

        val clientData = clientJson.toString().toByteArray()
        val response = JSONObject()
        if (clientDataHash == null) {
            response.put("clientDataJSON", b64Encode(clientData))
        }
        response.put("attestationObject", b64Encode(attestationObject))
        response.put("transports", JSONArray(listOf("internal", "hybrid")))

        addParsedAttestationObjectFieldsToJSON(
            authData(),
            getPublicKeyAlgorithm(),
            response,
        )

        return response
    }

    private fun getPublicKeyAlgorithm(): Long {
        // Learn more here : https://www.iana.org/assignments/cose/cose.xhtml#algorithms
        return -7
    }
}
