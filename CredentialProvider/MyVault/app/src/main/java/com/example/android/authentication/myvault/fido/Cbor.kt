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

import java.lang.IllegalArgumentException

const val TYPE_UNSIGNED_INT = 0x00
const val TYPE_NEGATIVE_INT = 0x01
const val TYPE_BYTE_STRING = 0x02
const val TYPE_TEXT_STRING = 0x03
const val TYPE_ARRAY = 0x04
const val TYPE_MAP = 0x05

/**
 * This class helps encode the data being sent to Relying party (Rps). WebAuthn uses CBOR serialization for binary data sent to the Relying Party.
 * This class is used for demonstration purpose and we don't recommend you to use it directly on production.
 * Please refer to standard WebAuthn specs for Cbor : https://www.w3.org/TR/webauthn-3/#cbor
 */
class Cbor {
    fun encode(data: Any): ByteArray {
        if (data is Number) {
            return if (data is Double) {
                throw IllegalArgumentException("Don't support doubles yet")
            } else {
                val value = data.toLong()
                if (value >= 0) {
                    createArg(TYPE_UNSIGNED_INT, value)
                } else {
                    createArg(TYPE_NEGATIVE_INT, -1 - value)
                }
            }
        }
        if (data is ByteArray) {
            return createArg(TYPE_BYTE_STRING, data.size.toLong()) + data
        }
        if (data is String) {
            return createArg(TYPE_TEXT_STRING, data.length.toLong()) + data.encodeToByteArray()
        }
        if (data is List<*>) {
            var ret = createArg(TYPE_ARRAY, data.size.toLong())
            for (i in data) {
                ret += encode(i!!)
            }
            return ret
        }
        if (data is Map<*, *>) {
            // Refer here: https://fidoalliance.org/specs/fido-v2.1-ps-20210615/fido-client-to-authenticator-protocol-v2.1-ps-20210615.html#ctap2-canonical-cbor-encoding-form
            var ret = createArg(TYPE_MAP, data.size.toLong())
            for (i in data) {
                ret += encode(i.key!!)
                ret += encode(i.value!!)
            }
            return ret
        }
        throw IllegalArgumentException("Bad type")
    }

    private fun createArg(type: Int, arg: Long): ByteArray {
        val t = type shl 5
        val a = arg.toInt()
        if (arg < 24) {
            return byteArrayOf(((t or a) and 0xFF).toByte())
        }
        if (arg <= 0xFF) {
            return byteArrayOf(
                ((t or 24) and 0xFF).toByte(),
                (a and 0xFF).toByte(),
            )
        }
        if (arg <= 0xFFFF) {
            return byteArrayOf(
                ((t or 25) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte(),
            )
        }
        if (arg <= 0xFFFFFFFF) {
            return byteArrayOf(
                ((t or 26) and 0xFF).toByte(),
                ((a shr 24) and 0xFF).toByte(),
                ((a shr 16) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte(),
            )
        }
        throw IllegalArgumentException("bad Arg")
    }
}
