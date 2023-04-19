/*
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.credentialmanager.sample.api

import android.util.JsonReader
import android.util.JsonToken.STRING
import android.util.JsonWriter
import android.util.Log
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
import com.google.credentialmanager.sample.BuildConfig
import com.google.credentialmanager.sample.api.ApiResult.SignedOutFromServer
import com.google.credentialmanager.sample.api.ApiResult.Success
import com.google.credentialmanager.sample.decodeBase64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.io.StringReader
import java.io.StringWriter
import javax.inject.Inject

/**
 * Interacts with the server API.
 */
class AuthApi @Inject constructor(
    private val client: OkHttpClient
) {

    companion object {
        private const val BASE_URL = BuildConfig.API_BASE_URL
        private val JSON = "application/json".toMediaTypeOrNull()
        private const val SessionIdKey = "connect.sid="
        private const val TAG = "AuthApi"
    }

    /**
     * @param username The username to be used for sign-in.
     * @return The Session ID.
     */
    suspend fun username(username: String): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/username").method("POST", jsonRequestBody {
                name("username").value(username)
            }).build()
        )
        val response = call.await()
        return response.result("Error calling /username") { }
    }

    /**
     * @param sessionId The session ID received on `username()`.
     * @param password A password.
     * @return An [ApiResult].
     */
    suspend fun password(sessionId: String, password: String): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/password").addHeader("Cookie", formatCookie(sessionId))
                .method("POST", jsonRequestBody {
                    name("password").value(password)
                }).build()
        )
        val response = call.await()
        return response.result("Error calling /password") { }
    }

    /**
     * @param sessionId The session ID.
     * @return a JSON object.
     */
    suspend fun registerRequest(sessionId: String): ApiResult<JSONObject> {
        val call = client.newCall(
            Builder().url("$BASE_URL/registerRequest").addHeader("Cookie", formatCookie(sessionId))
                .method("POST", jsonRequestBody {
                    name("attestation").value("none")
                    name("authenticatorSelection").objectValue {
                        name("authenticatorAttachment").value("platform")
                        name("userVerification").value("required")
                        name("requireResidentKey").value(true)
                        name("residentKey").value("required")

                    }
                }).build()
        )
        val response = call.await()

        return response.result("Error calling /registerRequest") {
            parsePublicKeyCredentialCreationOptions(
                body ?: throw ApiException("Empty response from /registerRequest")
            )
        }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param response JSONObject for Register response.
     * @param credentialId id to be used
     * @return A list of all the credentials registered on the server, including the newly
     * registered one.
     */
    suspend fun registerResponse(
        sessionId: String, response: JSONObject, credentialId: String
    ): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/registerResponse").addHeader("Cookie", formatCookie(sessionId))
                .method("POST", jsonRequestBody {
                    name("id").value(credentialId)
                    name("type").value(PUBLIC_KEY.toString())
                    name("rawId").value(credentialId)
                    name("response").objectValue {
                        name("clientDataJSON").value(
                            response.getString("clientDataJSON")
                        )
                        name("attestationObject").value(
                            response.getString("attestationObject")
                        )
                    }
                }).build()
        )
        val apiResponse = call.await()
        if (apiResponse.body == null) {
            throw ApiException("Empty response from /registerResponse")
        }
        return apiResponse.result("Error calling /registerResponse") {}
    }

    /**
     * @param sessionId The session ID.
     * @param credentialId The credential ID to be removed.
     */
    suspend fun removeKey(sessionId: String, credentialId: String): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/removeKey?credId=$credentialId")
                .addHeader("Cookie", formatCookie(sessionId)).method("POST", jsonRequestBody {})
                .build()
        )
        val response = call.await()
        return response.result("Error calling /removeKey") { }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param credentialId The credential ID of this device.
     * @return a JSON object.
     */
    suspend fun signinRequest(): ApiResult<JSONObject> {
        val call = client.newCall(Builder().url(buildString {
            append("$BASE_URL/signinRequest")
        }).method("POST", jsonRequestBody {})
            .build()
        )
        val response = call.await()
        return response.result("Error calling /signinRequest") {
            parsePublicKeyCredentialRequestOptions(
                body ?: throw ApiException("Empty response from /signinRequest")
            )
        }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param response The JSONObject for signInResponse.
     * @param credentialId id/rawId.
     * @return A list of all the credentials registered on the server, including the newly
     * registered one.
     */
    suspend fun signinResponse(
        sessionId: String, response: JSONObject, credentialId: String
    ): ApiResult<Unit> {

        val call = client.newCall(
            Builder().url("$BASE_URL/signinResponse").addHeader("Cookie", formatCookie(sessionId))
                .method("POST", jsonRequestBody {
                    name("id").value(credentialId)
                    name("type").value(PUBLIC_KEY.toString())
                    name("rawId").value(credentialId)
                    name("response").objectValue {
                        name("clientDataJSON").value(
                            response.getString("clientDataJSON")
                        )
                        name("authenticatorData").value(
                            response.getString("authenticatorData")
                        )
                        name("signature").value(
                            response.getString("signature")
                        )
                        name("userHandle").value(
                            response.getString("userHandle")
                        )
                    }
                }).build()
        )
        val apiResponse = call.await()
        return apiResponse.result("Error calling /signingResponse") {
        }
    }

    private fun parsePublicKeyCredentialRequestOptions(
        body: ResponseBody
    ): JSONObject {
        val jObject = JSONObject()
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "challenge" -> jObject.put("challenge", reader.nextString())
                    "userVerification" -> reader.skipValue()
                    "allowCredentials" -> jObject.put(
                        "allowCredentials",
                        parseCredentialDescriptors(reader)
                    )
                    "rpId" -> jObject.put("rpId", reader.nextString())
                    "timeout" -> jObject.put("timeout", reader.nextDouble())
                }
            }
            reader.endObject()
        }
        return jObject
    }

    private fun parsePublicKeyCredentialCreationOptions(
        body: ResponseBody
    ): JSONObject {
        val jObject = JSONObject()

        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "user" -> jObject.put("user", parseUser(reader))
                    "challenge" -> jObject.put("challenge", reader.nextString())
                    "pubKeyCredParams" -> jObject.put("pubKeyCredParams", parseParameters(reader))
                    "timeout" -> jObject.put("timeout", reader.nextDouble())
                    "attestation" -> reader.skipValue()
                    "excludeCredentials" -> jObject.put(
                        "excludeCredentials",
                        parseCredentialDescriptors(reader)
                    )
                    "authenticatorSelection" -> jObject.put(
                        "authenticatorSelection", parseSelection(reader)
                    )
                    "rp" -> jObject.put("rp", parseRp(reader))
                }
            }
            reader.endObject()
        }
        return jObject
    }

    private fun parseRp(reader: JsonReader): JSONObject {
        val obj = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> obj.put("id", reader.nextString())
                "name" -> obj.put("name", reader.nextString())
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return obj
    }

    private fun parseSelection(reader: JsonReader): JSONObject {
        val obj = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "authenticatorAttachment" -> obj.put(
                    "authenticatorAttachment",
                    Attachment.fromString(reader.nextString())
                )
                "residentKey" -> obj.put(
                    "residentKey", reader.nextString()
                )
                "requireResidentKey" -> obj.put(
                    "requireResidentKey", reader.nextBoolean()
                )
                "userVerification"
                -> obj.put(
                    "userVerification", reader.nextString()
                )
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return obj
    }

    private fun parseCredentialDescriptors(
        reader: JsonReader
    ): JSONArray {
        val jsonArray = JSONArray()
        var obj = JSONObject()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            while (reader.hasNext()) {
                obj = JSONObject()
                when (reader.nextName()) {
                    "id" -> obj.put("id", reader.nextString().decodeBase64())
                    "type" -> reader.skipValue()
                    "transports" -> reader.skipValue()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            jsonArray.put(obj)
        }
        reader.endArray()
        return jsonArray
    }

    private fun parseUser(reader: JsonReader): JSONObject {
        val jObject = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> jObject.put("id", reader.nextString())
                "name" -> jObject.put("name", reader.nextString())
                "displayName" -> jObject.put("displayName", reader.nextString())
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return jObject
    }

    private fun parseParameters(reader: JsonReader): JSONArray {
        val jsonArray = JSONArray()
        reader.beginArray()
        while (reader.hasNext()) {
            val obj = JSONObject()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> obj.put("type", reader.nextString())
                    "alg" -> obj.put("alg", reader.nextInt())
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            jsonArray.put(obj)
        }
        reader.endArray()
        return jsonArray
    }

    private fun jsonRequestBody(body: JsonWriter.() -> Unit): RequestBody {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            writer.body()
            writer.endObject()
        }
        return output.toString().toRequestBody(JSON)
    }

    private fun throwResponseError(response: Response, message: String): Nothing {
        val b = response.body
        if (b != null) {
            throw ApiException("$message; ${parseError(b)}")
        } else {
            throw ApiException(message)
        }
    }

    private fun parseError(body: ResponseBody): String {
        val errorString = body.string()
        try {
            JsonReader(StringReader(errorString)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == "error") {
                        val token = reader.peek()
                        if (token == STRING) {
                            return reader.nextString()
                        }
                        return "Unknown"
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot parse the error: $errorString", e)
            // Don't throw; this method is called during throwing.
        }
        return ""
    }

    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    private fun <T> Response.result(errorMessage: String, data: Response.() -> T): ApiResult<T> {
        if (!isSuccessful) {
            if (code == 401) { // Unauthorized
                return SignedOutFromServer
            }
            // All other errors throw an exception.
            throwResponseError(this, errorMessage)
        }
        val cookie = headers("set-cookie").find { it.startsWith(SessionIdKey) }
        val sessionId = if (cookie != null) parseSessionId(cookie) else null
        return Success(sessionId, data())
    }

    private fun parseSessionId(cookie: String): String {
        val start = cookie.indexOf(SessionIdKey)
        if (start < 0) {
            throw ApiException("Cannot find $SessionIdKey")
        }
        val semicolon = cookie.indexOf(";", start + SessionIdKey.length)
        val end = if (semicolon < 0) cookie.length else semicolon
        return cookie.substring(start + SessionIdKey.length, end)
    }

    private fun formatCookie(sessionId: String): String {
        return "$SessionIdKey$sessionId"
    }
}
