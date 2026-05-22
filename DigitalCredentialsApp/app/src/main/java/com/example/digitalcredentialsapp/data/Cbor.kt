/*
 * Copyright 2026 Google LLC
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

package com.example.digitalcredentialsapp.data

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

const val TYPE_UNSIGNED_INT = 0x00
const val TYPE_NEGATIVE_INT = 0x01
const val TYPE_BYTE_STRING = 0x02
const val TYPE_TEXT_STRING = 0x03
const val TYPE_ARRAY = 0x04
const val TYPE_MAP = 0x05
const val TYPE_TAG = 0x06
const val TYPE_SIMPLE = 0x07

/**
 * A utility class for encoding and decoding data in CBOR (Concise Binary Object Representation) format.
 *
 * This implementation is designed specifically for demonstration purposes when handling
 * Mobile Driver's License (mDL) credential responses.
 */
class Cbor {
    /**
     * Encodes a supported object into a CBOR [ByteArray].
     *
     * @param data The object to encode (Supports: Number, ByteArray, String, List, Map).
     * @return The encoded CBOR byte array.
     * @throws IllegalArgumentException if the data type is unsupported.
     */
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
            var ret = createArg(TYPE_MAP, data.size.toLong())
            for (i in data) {
                ret += encode(i.key!!)
                ret += encode(i.value!!)
            }
            return ret
        }
        throw IllegalArgumentException("Bad type")
    }

    /**
     * Helper to create the head/argument of a CBOR item.
     */
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

    /**
     * A decoder for CBOR-encoded data.
     *
     * @property buffer The [ByteBuffer] containing the CBOR data.
     */
    class Decoder(private val buffer: ByteBuffer) {
        constructor(bytes: ByteArray) : this(ByteBuffer.wrap(bytes))

        /**
         * Decodes the next item in the CBOR stream.
         *
         * @return The decoded object, or null if the buffer is empty.
         * @throws IllegalArgumentException for unsupported major types or malformed data.
         */
        fun decodeNext(): Any? {
            if (!buffer.hasRemaining()) return null
            val b = buffer.get().toInt() and 0xFF
            val majorType = b shr 5
            val additionalInfo = b and 0x1F

            return when (majorType) {
                TYPE_UNSIGNED_INT -> readLength(additionalInfo)
                TYPE_NEGATIVE_INT -> -1 - readLength(additionalInfo)
                TYPE_BYTE_STRING -> {
                    val length = readLength(additionalInfo).toInt()
                    val bytes = ByteArray(length)
                    buffer.get(bytes)
                    bytes
                }
                TYPE_TEXT_STRING -> {
                    val length = readLength(additionalInfo).toInt()
                    val bytes = ByteArray(length)
                    buffer.get(bytes)
                    String(bytes)
                }
                TYPE_ARRAY -> {
                    val size = readLength(additionalInfo).toInt()
                    val list = mutableListOf<Any?>()
                    repeat(size) {
                        list.add(decodeNext())
                    }
                    list
                }
                TYPE_MAP -> {
                    val size = readLength(additionalInfo).toInt()
                    val map = mutableMapOf<Any?, Any?>()
                    repeat(size) {
                        val key = decodeNext()
                        val value = decodeNext()
                        map[key] = value
                    }
                    map
                }
                TYPE_TAG -> {
                    val tag = readLength(additionalInfo)
                    if (tag == 24L) {
                        // ISO 18013-5 unwrapping of Tag 24 items
                        val innerBytes = decodeNext() as? ByteArray
                            ?: throw IllegalArgumentException("Tag 24 must be followed by byte string")
                        Decoder(innerBytes).decodeNext()
                    } else {
                        decodeNext()
                    }
                }
                TYPE_SIMPLE -> {
                    when (additionalInfo) {
                        20 -> false
                        21 -> true
                        22 -> null
                        else -> null
                    }
                }
                else -> throw IllegalArgumentException("Unsupported major type: $majorType")
            }
        }

        private fun readLength(info: Int): Long {
            return when (info) {
                in 0..23 -> info.toLong()
                24 -> buffer.get().toLong() and 0xFF
                25 -> buffer.short.toLong() and 0xFFFF
                26 -> buffer.int.toLong() and 0xFFFFFFFFL
                27 -> buffer.long
                else -> throw IllegalArgumentException("Invalid length info: $info")
            }
        }
    }
}
