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
package com.authentication.shrine.utility

import java.text.SimpleDateFormat
import java.util.Date
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun Long.toReadableDate(): String {
    val dateFormat = SimpleDateFormat("dd-MMM-yyyy")
    val dateString = dateFormat.format(Date(this))
    return dateString
}

@OptIn(ExperimentalEncodingApi::class)
fun String.toImageSvgString(): String? {
    val headerString = "data:image/svg+xml;base64,"
    if (indexOf(headerString) != -1) {
        val base64String = subSequence(headerString.length, length).toString()
        val decodedSvgString = Base64.decode(base64String).decodeToString()
        return decodedSvgString
    } else {
        return null
    }
}
