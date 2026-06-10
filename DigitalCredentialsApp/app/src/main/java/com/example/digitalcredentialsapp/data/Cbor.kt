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

import java.io.ByteArrayOutputStream

/**
 * Represents a CBOR tag and its associated item.
 *
 * @property tag The CBOR tag value.
 * @property item The item associated with the tag.
 */
data class CborTag(
    val tag: Long,
    val item: Any?
)

/**
 * Decodes a CBOR-encoded byte array into its corresponding Kotlin object representation.
 *
 * @param data The CBOR-encoded byte array.
 * @return The decoded object (e.g., Long, String, ByteArray, List, Map, CborTag, or null).
 */
fun cborDecode(data: ByteArray): Any? {
    return Cbor().decode(data)
}

/**
 * Encodes a Kotlin object into a CBOR byte array.
 *
 * @param data The object to encode. Supports Number, String, ByteArray, List, Map, CborTag, and null.
 * @return The CBOR-encoded byte array.
 */
fun cborEncode(data: Any?): ByteArray {
    return Cbor().encode(data)
}

/**
 * A lightweight CBOR (Concise Binary Object Representation) encoder and decoder implementation.
 * Supports basic types, collections, and tags with security depth limits and bounds checking.
 */
class Cbor {
    /**
     * Internal representation of a decoded CBOR item.
     *
     * @property item The decoded object.
     * @property len The number of bytes consumed for this item.
     * @property type The CBOR major type.
     */
    data class Item(val item: Any?, val len: Int, val type: Int)

    /**
     * Internal representation of a CBOR argument (value/length) and its header length.
     *
     * @property arg The value or length parsed from the header.
     * @property len The length of the header in bytes.
     */
    data class Arg(val arg: Long, val len: Int)

    private val MAX_RECURSION_DEPTH = 32
    private val MAX_ITEM_SIZE = 1024 * 1024 // 1MB limit for individual strings/byte arrays

    /** Major type 0: Unsigned integer. */
    val TYPE_UNSIGNED_INT = 0x00
    /** Major type 1: Negative integer. */
    val TYPE_NEGATIVE_INT = 0x01
    /** Major type 2: Byte string. */
    val TYPE_BYTE_STRING = 0x02
    /** Major type 3: Text string. */
    val TYPE_TEXT_STRING = 0x03
    /** Major type 4: Array of items. */
    val TYPE_ARRAY = 0x04
    /** Major type 5: Map of pairs. */
    val TYPE_MAP = 0x05
    /** Major type 6: Optional semantic tag. */
    val TYPE_TAG = 0x06
    /** Major type 7: Floating-point, simple values, and null/bool. */
    val TYPE_FLOAT = 0x07

    /**
     * Decodes the provided CBOR byte array.
     *
     * @param data The byte array to decode.
     * @return The decoded object.
     * @throws IllegalArgumentException if the data is malformed or exceeds safety limits.
     */
    fun decode(data: ByteArray): Any? {
        val ret = parseItem(data, 0, 0)
        return ret.item
    }

    /**
     * Encodes the provided object into CBOR format.
     *
     * @param data The object to encode.
     * @return The encoded byte array.
     * @throws IllegalArgumentException if the type is unsupported or recursion depth is exceeded.
     */
    fun encode(data: Any?): ByteArray {
        val out = ByteArrayOutputStream()
        encodeInternal(data, out, 0)
        return out.toByteArray()
    }

    /**
     * Internal recursive encoding function.
     *
     * @param data The object to encode.
     * @param out The stream to write encoded bytes to.
     * @param depth Current recursion depth.
     */
    private fun encodeInternal(data: Any?, out: ByteArrayOutputStream, depth: Int) {
        if (depth > MAX_RECURSION_DEPTH) {
            throw IllegalArgumentException("Max recursion depth exceeded during encoding")
        }
        if (data == null) {
            out.write(createArg(TYPE_FLOAT, 22))
            return
        }
        if (data is Number) {
            if (data is Double) {
                throw IllegalArgumentException("Don't support doubles yet")
            } else {
                val value = data.toLong()
                if (value >= 0) {
                    out.write(createArg(TYPE_UNSIGNED_INT, value))
                    return
                } else {
                    out.write(createArg(TYPE_NEGATIVE_INT, -1 - value))
                    return
                }
            }
        }
        if (data is ByteArray) {
            out.write(createArg(TYPE_BYTE_STRING, data.size.toLong()))
            out.write(data)
            return
        }
        if (data is String) {
            val bytes = data.encodeToByteArray()
            out.write(createArg(TYPE_TEXT_STRING, bytes.size.toLong()))
            out.write(bytes)
            return
        }
        if (data is List<*>) {
            out.write(createArg(TYPE_ARRAY, data.size.toLong()))
            for (i in data) {
                encodeInternal(i, out, depth + 1)
            }
            return
        }
        if (data is Map<*, *>) {
            out.write(createArg(TYPE_MAP, data.size.toLong()))
            for (i in data) {
                encodeInternal(i.key!!, out, depth + 1)
                encodeInternal(i.value!!, out, depth + 1)
            }
            return
        }
        if (data is CborTag) {
            out.write(createArg(TYPE_TAG, data.tag))
            encodeInternal(data.item, out, depth + 1)
            return
        }
        throw IllegalArgumentException("Bad type")
    }

    /**
     * Extracts the major type from the CBOR header at the specified offset.
     */
    private fun getType(data: ByteArray, offset: Int): Int {
        val d = data[offset].toInt()
        return (d and 0xFF) shr 5
    }

    /**
     * Parses the argument (value or length) from the CBOR header.
     *
     * @param data The byte array being parsed.
     * @param offset The starting position of the header.
     * @return An [Arg] containing the parsed value and the header length.
     * @throws IllegalArgumentException if the header is truncated or uses unsupported 64-bit sizes.
     */
    private fun getArg(data: ByteArray, offset: Int): Arg {
        if (offset >= data.size) {
            throw IllegalArgumentException("Offset out of bounds")
        }
        val arg = data[offset].toLong() and 0x1F
        if (arg < 24) {
            return Arg(arg, 1)
        }
        if (arg == 24L) {
            if (offset + 1 >= data.size) throw IllegalArgumentException("Unexpected end of data")
            return Arg(data[offset + 1].toLong() and 0xFF, 2)
        }
        if (arg == 25L) {
            if (offset + 2 >= data.size) throw IllegalArgumentException("Unexpected end of data")
            var ret = (data[offset + 1].toLong() and 0xFF) shl 8
            ret = ret or (data[offset + 2].toLong() and 0xFF)
            return Arg(ret, 3)
        }
        if (arg == 26L) {
            if (offset + 4 >= data.size) throw IllegalArgumentException("Unexpected end of data")
            var ret = (data[offset + 1].toLong() and 0xFF) shl 24
            ret = ret or ((data[offset + 2].toLong() and 0xFF) shl 16)
            ret = ret or ((data[offset + 3].toLong() and 0xFF) shl 8)
            ret = ret or (data[offset + 4].toLong() and 0xFF)
            return Arg(ret, 5)
        }
        if (arg == 27L) {
            if (offset + 8 >= data.size) throw IllegalArgumentException("Unexpected end of data")
            var ret = (data[offset + 1].toLong() and 0xFF) shl 56
            ret = ret or ((data[offset + 2].toLong() and 0xFF) shl 48)
            ret = ret or ((data[offset + 3].toLong() and 0xFF) shl 40)
            ret = ret or ((data[offset + 4].toLong() and 0xFF) shl 32)
            ret = ret or ((data[offset + 5].toLong() and 0xFF) shl 24)
            ret = ret or ((data[offset + 6].toLong() and 0xFF) shl 16)
            ret = ret or ((data[offset + 7].toLong() and 0xFF) shl 8)
            ret = ret or (data[offset + 8].toLong() and 0xFF)
            if (ret < 0) throw IllegalArgumentException("Unsupported 64-bit arg size")
            return Arg(ret, 9)
        }
        throw IllegalArgumentException("Bad arg $arg")
    }

    /**
     * Recursively parses a CBOR item from the byte array.
     *
     * @param data The byte array to parse from.
     * @param offset Current position in the byte array.
     * @param depth Current recursion depth.
     * @return The parsed [Item].
     */
    private fun parseItem(data: ByteArray, offset: Int, depth: Int): Item {
        if (depth > MAX_RECURSION_DEPTH) {
            throw IllegalArgumentException("Max recursion depth exceeded")
        }
        val itemType = getType(data, offset)
        val arg = getArg(data, offset)

        when (itemType) {
            TYPE_UNSIGNED_INT -> {
                return Item(arg.arg, arg.len, TYPE_UNSIGNED_INT)
            }

            TYPE_NEGATIVE_INT -> {
                return Item(-1 - arg.arg, arg.len, TYPE_NEGATIVE_INT)
            }

            TYPE_BYTE_STRING -> {
                if (arg.arg > MAX_ITEM_SIZE) throw IllegalArgumentException("Byte string too large")
                val end = offset + arg.len + arg.arg.toInt()
                if (end > data.size) throw IllegalArgumentException("Unexpected end of data for byte string")
                val ret =
                    data.sliceArray(offset + arg.len until end)
                return Item(ret, arg.len + arg.arg.toInt(), TYPE_BYTE_STRING)
            }

            TYPE_TEXT_STRING -> {
                if (arg.arg > MAX_ITEM_SIZE) throw IllegalArgumentException("Text string too large")
                val end = offset + arg.len + arg.arg.toInt()
                if (end > data.size) throw IllegalArgumentException("Unexpected end of data for text string")
                val ret =
                    data.sliceArray(offset + arg.len until end)
                return Item(
                    ret.toString(Charsets.UTF_8), arg.len + arg.arg.toInt(),
                    TYPE_TEXT_STRING
                )
            }

            TYPE_ARRAY -> {
                val ret = mutableListOf<Any?>()
                var consumed = arg.len
                for (i in 0 until arg.arg.toInt()) {
                    if (offset + consumed >= data.size) throw IllegalArgumentException("Unexpected end of data for array")
                    val item = parseItem(data, offset + consumed, depth + 1)
                    ret.add(item.item)
                    consumed += item.len
                }
                return Item(ret.toList(), consumed, TYPE_ARRAY)
            }

            TYPE_MAP -> {
                val ret = mutableMapOf<Any?, Any?>()
                var consumed = arg.len
                for (i in 0 until arg.arg.toInt()) {
                    if (offset + consumed >= data.size) throw IllegalArgumentException("Unexpected end of data for map key")
                    val key = parseItem(data, offset + consumed, depth + 1)
                    consumed += key.len
                    if (offset + consumed >= data.size) throw IllegalArgumentException("Unexpected end of data for map value")
                    val value = parseItem(data, offset + consumed, depth + 1)
                    consumed += value.len
                    ret[key.item] = value.item
                }
                return Item(ret.toMap(), consumed, TYPE_MAP)
            }

            TYPE_TAG -> {
                if (offset + arg.len >= data.size) throw IllegalArgumentException("Unexpected end of data for tag")
                val tagItem = parseItem(data, offset + arg.len, depth + 1)
                return Item(CborTag(arg.arg, tagItem.item), arg.len + tagItem.len, TYPE_TAG)
            }

            TYPE_FLOAT -> {
                if (arg.arg.toInt() == 22) {
                    return Item(null, arg.len, TYPE_FLOAT)
                } else if (arg.arg.toInt() == 20) {
                    return Item(false, arg.len, TYPE_FLOAT)
                } else if (arg.arg.toInt() == 21) {
                    return Item(true, arg.len, TYPE_FLOAT)
                } else {
                    throw IllegalArgumentException("Bad float $arg")
                }
            }

            else -> {
                throw IllegalArgumentException("Bad type")
            }
        }
    }

    /**
     * Creates a CBOR header (major type + value/length argument).
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
                (a and 0xFF).toByte()
            )
        }
        if (arg <= 0xFFFF) {
            return byteArrayOf(
                ((t or 25) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte()
            )
        }
        if (arg <= 0xFFFFFFFF) {
            return byteArrayOf(
                ((t or 26) and 0xFF).toByte(),
                ((a shr 24) and 0xFF).toByte(),
                ((a shr 16) and 0xFF).toByte(),
                ((a shr 8) and 0xFF).toByte(),
                (a and 0xFF).toByte()
            )
        }
        throw IllegalArgumentException("bad Arg")
    }
}
