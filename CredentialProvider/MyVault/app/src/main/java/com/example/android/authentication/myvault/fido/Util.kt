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

import android.util.Base64
import androidx.credentials.provider.CallingAppInfo
import java.security.MessageDigest

/**
 * Decodes a Base64-encoded string into a byte array.
 *
 * @param str The Base64-encoded string.
 * @return The decoded byte array.
 */
fun b64Decode(str: String): ByteArray {
    return Base64.decode(str, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
}

/**
 * Encodes a byte array into a Base64-encoded string.
 *
 * @param data The byte array to encode.
 * @return The Base64-encoded string.
 */
fun b64Encode(data: ByteArray): String {
    return Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
}

/**
 * Generates an origin string for a given CallingAppInfo object.
 *
 * @param info The CallingAppInfo object.
 * @return The origin string.
 * refer the origin documentation for explanation : https://developer.android.com/training/sign-in/passkeys#verify-origin
 *
 */
fun appInfoToOrigin(info: CallingAppInfo): String {
    val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val certHash = md.digest(cert)
    return "android:apk-key-hash:${b64Encode(certHash)}"
}
