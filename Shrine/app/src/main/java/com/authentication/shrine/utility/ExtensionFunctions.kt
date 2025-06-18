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

import com.authentication.shrine.api.ApiException
import com.authentication.shrine.utility.Constants.SESSION_ID_KEY
import com.google.gson.Gson
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date

fun Long.toReadableDate(): String {
    val dateFormat = SimpleDateFormat("dd-MMM-yyyy")
    val dateString = dateFormat.format(Date(this))
    return dateString
}

fun <T> Response<T>.getSessionId(): String? {
    val cookie = headers()["set-cookie"]
    if (cookie != null) {
        val start = cookie.indexOf(SESSION_ID_KEY)
        if (start < 0) {
            throw ApiException("Cannot find Session ID")
        }
        val semicolon = cookie.indexOf(";", start + SESSION_ID_KEY.length)
        val end = if (semicolon < 0) cookie.length else semicolon
        return cookie.substring(start + SESSION_ID_KEY.length, end)
    } else {
        return null
    }
}

fun String.createCookieHeader(): String {
    return SESSION_ID_KEY + this
}

fun <T> Response<T>.getJsonObject(): JSONObject {
    val jsonString = Gson().toJson(body())
    return JSONObject(jsonString)
}
