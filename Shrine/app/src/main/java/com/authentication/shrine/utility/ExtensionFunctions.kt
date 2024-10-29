/*
 * Copyright 2025 The Android Open Source Project
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

/**
 * Extension function to convert a Long timestamp (in milliseconds since epoch)
 * to a human-readable date string in "dd-MMM-yyyy" format.
 *
 * Example: `1678886400000.toReadableDate()` might return "15-Mar-2023".
 *
 * @receiver The Long timestamp in milliseconds.
 * @return The formatted date string (e.g., "15-Mar-2023").
 * @see SimpleDateFormat
 */
fun Long.toReadableDate(): String {
    val dateFormat = SimpleDateFormat("dd-MMM-yyyy")
    val dateString = dateFormat.format(Date(this))
    return dateString
}

/**
 * Extension function to extract a session ID from the "set-cookie" header of a Retrofit {@link Response}.
 * It specifically looks for a cookie matching the {@code SESSION_ID_KEY}.
 *
 * @receiver The Retrofit {@link Response} object.
 * @param T The type of the response body.
 * @return The session ID string if found, or null if the "set-cookie" header is not present
 *         or the session ID cookie is not found.
 * @throws ApiException if the "set-cookie" header is present but the session ID key cannot be found within it.
 */
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

/**
 * Extension function to create a cookie header string from a session ID string.
 * This prepends the {@code SESSION_ID_KEY} to the given session ID.
 *
 * Example: `"myActualSessionId".createCookieHeader()` might return "session_id=myActualSessionId".
 *
 * @receiver The session ID string.
 * @return The formatted cookie header string.
 */
fun String.createCookieHeader(): String {
    return SESSION_ID_KEY + this
}

/**
 * Extension function to convert the body of a Retrofit {@link Response} into a {@link JSONObject}.
 * This function uses Gson to first serialize the response body to a JSON string,
 * and then parses that string into a JSONObject.
 *
 * Note: This approach involves an intermediate JSON string representation. If performance
 * is critical for very large objects, or if the response body is already a JSON string,
 * more direct methods might be considered.
 *
 * @receiver The Retrofit {@link Response} object.
 * @param T The type of the response body, which must be serializable by Gson.
 * @return A {@link JSONObject} representation of the response body.
 *         Returns an empty JSONObject if the body is null or cannot be parsed.
 * @see Gson
 * @see JSONObject
 */
fun <T> Response<T>.getJsonObject(): JSONObject {
    val jsonString = Gson().toJson(body())
    return JSONObject(jsonString)
}
