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
package com.authentication.shrine.api

import android.util.JsonReader
import android.util.JsonToken.STRING
import android.util.JsonWriter
import android.util.Log
import com.authentication.shrine.BuildConfig
import com.authentication.shrine.api.ApiResult.SignedOutFromServer
import com.authentication.shrine.api.ApiResult.Success
import com.authentication.shrine.decodeBase64
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
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
 * Class for interacting with an authentication server.
 *
 * @param client The OkHttpClient instance to use for making HTTP requests.
 */
class AuthApi @Inject constructor(
    private val client: OkHttpClient,
) {

    companion object {
        /**
         * The base URL of the authentication server.
         */
        private const val BASE_URL = BuildConfig.API_BASE_URL

        /**
         * The media type for JSON requests.
         */
        private val JSON = "application/json".toMediaTypeOrNull()

        /**
         * The key used for the session ID cookie.
         */
        private const val SESSION_ID_KEY = "SESAME_SESSION_COOKIE="

        /**
         * The tag for logging.
         */
        private const val TAG = "AuthApi"

        // String constants for JSON objects
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
    }

    /**
     * Sends a username to the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    suspend fun setUsername(username: String): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/auth/username").method(
                "POST",
                createJSONRequestBody {
                    name(USERNAME).value(username)
                },
            ).build(),
        )
        val response = call.await()
        return response.result(errorMessage = "Error setting username") { }
    }

    /**
     * Sends a password to the authentication server.
     *
     * @param sessionId The session ID received from `username()`.
     * @param password A password.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    suspend fun setPassword(sessionId: String, password: String): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/auth/password").addHeader("Cookie", formatCookie(sessionId))
                .method(
                    "POST",
                    createJSONRequestBody {
                        name(PASSWORD).value(password)
                    },
                ).build(),
        )
        val response = call.await()
        return response.result(errorMessage = "Error setting password") { }
    }

    /**
     * Requests a public key credential creation options object from the authentication server.
     *
     * @param sessionId The session ID.
     * @return An [ApiResult] containing the public key credential creation options object.
     */
    suspend fun registerPasskeyCreationRequest(sessionId: String): ApiResult<JSONObject> {
        val call = client.newCall(
            Builder().url("$BASE_URL/webauthn/registerRequest").addHeader("Cookie", formatCookie(sessionId))
                .method(
                    "POST",
                    createJSONRequestBody {
                        name("attestation").value("none")
                        name("authenticatorSelection").objectValue {
                            name("authenticatorAttachment").value("platform")
                            name("userVerification").value("required")
                            name("requireResidentKey").value(true)
                            name("residentKey").value("required")
                        }
                    },
                ).build(),
        )
        val response = call.await()

        return response.result(errorMessage = "Error registering Passkey Creation Request") {
            parsePublicKeyCredentialCreationOptions(
                body ?: throw ApiException("Empty response from registerRequest API"),
            )
        }
    }

    /**
     * Sends a public key credential to the authentication server.
     *
     * @param sessionId The session ID to be used for the sign-in.
     * @param response The public key credential creation options object.
     * @param credentialId The ID of the credential.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    suspend fun registerPasskeyCreationResponse(
        sessionId: String,
        response: JSONObject,
        credentialId: String,
    ): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/webauthn/registerResponse").addHeader("Cookie", formatCookie(sessionId))
                .method(
                    "POST",
                    createJSONRequestBody {
                        name("id").value(credentialId)
                        name("type").value(PUBLIC_KEY.toString())
                        name("rawId").value(credentialId)
                        name("response").objectValue {
                            name("clientDataJSON").value(
                                response.getString("clientDataJSON"),
                            )
                            name("attestationObject").value(
                                response.getString("attestationObject"),
                            )
                        }
                    },
                ).build(),
        )
        val apiResponse = call.await()
        if (apiResponse.body == null) {
            throw ApiException(message = "Empty response from registerResponse API")
        }
        return apiResponse.result(errorMessage = "Error registering Passkey Creation Response") {}
    }

    /**
     * Initiates the sign-in flow using passkeys.
     *
     * @return An [ApiResult] containing the public key credential request options, or an error if the API call fails.
     */
    suspend fun signInWithPasskeysRequest(): ApiResult<JSONObject> {
        val call = client.newCall(
            Builder().url(
                buildString {
                    append("$BASE_URL/webauthn/signinRequest")
                },
            ).method("POST", createJSONRequestBody {})
                .build(),
        )
        val response = call.await()
        return response.result(errorMessage = "Error in SignIn with Passkeys Request") {
            parsePublicKeyCredentialRequestOptions(
                body ?: throw ApiException(message = "Empty response from signInRequest API"),
            )
        }
    }

    /**
     * Sends a public key credential to the authentication server for sign-in.
     *
     * @param sessionId The session ID to be used for the sign-in.
     * @param response The public key credential request options object.
     * @param credentialId The ID of the credential.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    suspend fun signInWithPasskeysResponse(
        sessionId: String,
        response: JSONObject,
        credentialId: String,
    ): ApiResult<Unit> {
        val call = client.newCall(
            Builder().url("$BASE_URL/webauthn/signinResponse").addHeader("Cookie", formatCookie(sessionId))
                .method(
                    "POST",
                    createJSONRequestBody {
                        name("id").value(credentialId)
                        name("type").value(PUBLIC_KEY.toString())
                        name("rawId").value(credentialId)
                        name("response").objectValue {
                            name("clientDataJSON").value(response.getString("clientDataJSON"))
                            name("authenticatorData").value(response.getString("authenticatorData"))
                            name("signature").value(response.getString("signature"))
                            name("userHandle").value(response.getString("userHandle"))
                        }
                    },
                ).build(),
        )
        val apiResponse = call.await()
        return apiResponse.result(errorMessage = "Error in SignIn Response") { }
    }

    /**
     * Parses a public key credential request options object from a JSON response.
     *
     * @param responseBody The JSON response body.
     * @return A [JSONObject] containing the parsed public key credential request options.
     */
    private fun parsePublicKeyCredentialRequestOptions(
        responseBody: ResponseBody,
    ): JSONObject {
        val credentialRequestOptions = JSONObject()
        JsonReader(responseBody.byteStream().bufferedReader()).use { jsonReader ->
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "challenge" -> credentialRequestOptions.put("challenge", jsonReader.nextString())
                    "userVerification" -> jsonReader.skipValue()
                    "allowCredentials" -> credentialRequestOptions.put(
                        "allowCredentials",
                        parseCredentialDescriptors(jsonReader),
                    )

                    "rpId" -> credentialRequestOptions.put("rpId", jsonReader.nextString())
                    "timeout" -> credentialRequestOptions.put("timeout", jsonReader.nextDouble())
                }
            }
            jsonReader.endObject()
        }
        return credentialRequestOptions
    }

    /**
     * Parses a public key credential creation options object from a JSON response.
     *
     * @param body The JSON response body.
     * @return A [JSONObject] containing the parsed public key credential creation options.
     */
    private fun parsePublicKeyCredentialCreationOptions(
        body: ResponseBody,
    ): JSONObject {
        val credentialCreationOptions = JSONObject()

        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "user" -> {
                        credentialCreationOptions.put("user", parseUser(reader))
                    }

                    "challenge" -> {
                        credentialCreationOptions.put("challenge", reader.nextString())
                    }

                    "pubKeyCredParams" -> {
                        credentialCreationOptions.put(
                            "pubKeyCredParams",
                            parsePubKeyCredParameters(reader),
                        )
                    }

                    "timeout" -> {
                        credentialCreationOptions.put(
                            "timeout",
                            reader.nextDouble(),
                        )
                    }

                    "attestation" -> {
                        reader.skipValue()
                    }

                    "extensions" -> {
                        reader.skipValue()
                    }

                    "excludeCredentials" -> {
                        credentialCreationOptions.put(
                            "excludeCredentials",
                            parseCredentialDescriptors(reader),
                        )
                    }

                    "authenticatorSelection" -> {
                        credentialCreationOptions.put(
                            "authenticatorSelection",
                            parseSelection(reader),
                        )
                    }

                    "rp" -> {
                        credentialCreationOptions.put(
                            "rp",
                            parseRp(reader),
                        )
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return credentialCreationOptions
    }

    /**
     * Parses the `rp` object from the JSON response.
     *
     * @param jsonReader The JSON reader.
     * @return A [JSONObject] containing the parsed `rp` object.
     */
    private fun parseRp(jsonReader: JsonReader): JSONObject {
        val jsonObject = JSONObject()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "id" -> jsonObject.put("id", jsonReader.nextString())
                "name" -> jsonObject.put("name", jsonReader.nextString())
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return jsonObject
    }

    /**
     * Parses the `authenticatorSelection` object from the JSON response.
     *
     * @param jsonReader The JSON reader.
     * @return A [JSONObject] containing the parsed `authenticatorSelection` object.
     */
    private fun parseSelection(jsonReader: JsonReader): JSONObject {
        val jsonObject = JSONObject()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "authenticatorAttachment" -> jsonObject.put(
                    "authenticatorAttachment",
                    Attachment.fromString(jsonReader.nextString()),
                )

                "residentKey" -> jsonObject.put(
                    "residentKey",
                    jsonReader.nextString(),
                )

                "requireResidentKey" -> jsonObject.put(
                    "requireResidentKey",
                    jsonReader.nextBoolean(),
                )

                "userVerification" -> jsonObject.put(
                    "userVerification",
                    jsonReader.nextString(),
                )

                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return jsonObject
    }

    /**
     * Parses the `credentialDescriptors` array from the JSON response.
     *
     * @param jsonReader The JSON reader.
     * @return A [JSONArray] containing the parsed `credentialDescriptors`.
     */
    private fun parseCredentialDescriptors(
        jsonReader: JsonReader,
    ): JSONArray {
        val jsonArray = JSONArray()
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            val jsonObject = JSONObject()
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "id" -> jsonObject.put("id", jsonReader.nextString().decodeBase64())
                    "type" -> jsonReader.skipValue()
                    "transports" -> jsonReader.skipValue()
                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()
            if (jsonObject.length() != 0) {
                jsonArray.put(jsonObject)
            }
        }
        jsonReader.endArray()
        return jsonArray
    }

    /**
     * Parses the `user` object from the JSON response.
     *
     * @param jsonReader The JSON reader.
     * @return A [JSONObject] containing the parsed `user` object.
     */
    private fun parseUser(jsonReader: JsonReader): JSONObject {
        val userObjectJSON = JSONObject()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "id" -> userObjectJSON.put("id", jsonReader.nextString())
                "name" -> userObjectJSON.put("name", jsonReader.nextString())
                "displayName" -> userObjectJSON.put("displayName", jsonReader.nextString())
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return userObjectJSON
    }

    /**
     * Parses the `pubKeyCredParams` array from the JSON response.
     *
     * @param jsonReader The JSON reader.
     * @return A [JSONArray] containing the parsed `pubKeyCredParams`.
     */
    private fun parsePubKeyCredParameters(jsonReader: JsonReader): JSONArray {
        val publicKeyCredentialParamsJSON = JSONArray()
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            val jsonObject = JSONObject()
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "type" -> jsonObject.put("type", jsonReader.nextString())
                    "alg" -> jsonObject.put("alg", jsonReader.nextInt())
                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()
            publicKeyCredentialParamsJSON.put(jsonObject)
        }
        jsonReader.endArray()
        return publicKeyCredentialParamsJSON
    }

    /**
     * Creates a JSON request body from the given lambda expression.
     *
     * @param body A lambda expression that writes to the [JsonWriter].
     * @return A [RequestBody] containing the JSON data.
     */
    private fun createJSONRequestBody(body: JsonWriter.() -> Unit): RequestBody {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            writer.body()
            writer.endObject()
        }
        return output.toString().toRequestBody(JSON)
    }

    /**
     * Throws an [ApiException] based on the given response and message.
     *
     * @param response The response object.
     * @param message The error message.
     */
    private fun throwResponseError(response: Response, message: String): Nothing {
        val responseBody = response.body
        if (responseBody != null) {
            throw ApiException("$message; ${parseError(responseBody)}")
        } else {
            throw ApiException(message)
        }
    }

    /**
     * Parses the error message from the given response body.
     *
     * @param body The response body.
     * @return The error message, or an empty string if it cannot be parsed.
     */
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

    /**
     * Extension function for [JsonWriter] to write an object value.
     *
     * @param body A lambda expression that writes to the [JsonWriter].
     */
    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    /**
     * Extension function for [Response] to convert it to an [ApiResult].
     *
     * @param errorMessage The error message to use if the response is not successful.
     * @param data A lambda expression that extracts the data from the response.
     * @return An [ApiResult] containing the data or an error.
     */
    private fun <T> Response.result(errorMessage: String, data: Response.() -> T): ApiResult<T> {
        if (!isSuccessful) {
            if (code == 401) { // Unauthorized
                return SignedOutFromServer
            }
            // All other errors throw an exception.
            throwResponseError(this, errorMessage)
        }
        val cookie = headers("set-cookie").find { it.startsWith(SESSION_ID_KEY) }
        val sessionId = if (cookie != null) parseSessionId(cookie) else null
        return Success(sessionId, data())
    }

    /**
     * Parses the session ID from the given cookie.
     *
     * @param cookie The cookie string.
     * @return The session ID.
     */
    private fun parseSessionId(cookie: String): String {
        val start = cookie.indexOf(SESSION_ID_KEY)
        if (start < 0) {
            throw ApiException("Cannot find $SESSION_ID_KEY")
        }
        val semicolon = cookie.indexOf(";", start + SESSION_ID_KEY.length)
        val end = if (semicolon < 0) cookie.length else semicolon
        return cookie.substring(start + SESSION_ID_KEY.length, end)
    }

    /**
     * Formats the session ID into a cookie string.
     *
     * @param sessionId The session ID.
     * @return The cookie string.
     */
    private fun formatCookie(sessionId: String): String {
        return "$SESSION_ID_KEY$sessionId"
    }
}
