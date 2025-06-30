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
package com.authentication.shrinewear.authenticator

import android.os.Build
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken.STRING
import android.util.JsonWriter
import android.util.Log
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import com.authentication.shrinewear.BuildConfig
import com.authentication.shrinewear.api.AddHeaderInterceptor
import com.authentication.shrinewear.api.ApiException
import com.authentication.shrinewear.api.ApiResult
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Todo(reader): Add your server client ID here.
const val SERVER_CLIENT_ID = ""

/**
 * Manages all client-side interactions with the authentication backend server.
 *
 * This class serves as the primary interface for authenticating users using various credential types,
 * including Passkeys (WebAuthn), traditional username/password, and Google ID Tokens. It handles
 * sending credential data to the server, managing the session ID received from successful authentication,
 * and parsing server responses into an [ApiResult] for consumption by higher-level logic.
 *
 * It utilizes [OkHttpClient] for network operations and includes
 * helper methods for JSON request/response processing and error handling.
 */
class AuthenticationServer {
    private val signedInState = MutableStateFlow(false)
    private val httpClient: OkHttpClient
    private var sessionId: String? = null

    companion object {
        /**
         * The tag for logging.
         */
        private const val TAG = "AuthApi"

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

        // String constants for JSON objects
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
    }

    init {
        val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"
        httpClient = OkHttpClient.Builder()
            .addInterceptor(AddHeaderInterceptor(userAgent))
            .addInterceptor(
                HttpLoggingInterceptor { message ->
                    println("LOG-APP: $message")
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrieves the public key credential request options from the authentication server.
     *
     * This method fetches the necessary challenge and parameters from the backend
     * to initiate a Passkey (WebAuthn) sign-in flow. On successful retrieval,
     * it updates the internal session ID.
     *
     * @return A JSON string containing the public key request options, or an empty string
     * if the server indicates a sign-out state or an error occurs during retrieval.
     */
    suspend fun getPublicKeyRequestOptions(): String {
        return when (val publicKeyRequestOptions = retrievePublicKeyRequestOptions()) {
            is ApiResult.Success -> {
                publicKeyRequestOptions.sessionId?.let { newSessionId ->
                    sessionId = newSessionId
                }
                publicKeyRequestOptions.data.toString()
            }

            is ApiResult.SignedOutFromServer -> {
                signOut()
                ""
            }
        }
    }

    /**
     * Retrieves passkeys public key request options from the auth server, if they exist.
     *
     * @return An [ApiResult] containing the public key
     * credential request options, or an error if the API call fails.
     */
    private suspend fun retrievePublicKeyRequestOptions(): ApiResult<JSONObject> {
        val httpResponse = httpClient.newCall(
            okhttp3.Request.Builder().url(
                buildString { append("$BASE_URL/webauthn/signinRequest") },
            ).method("POST", createJSONRequestBody {}).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error in SignIn with Passkeys Request") {
            parsePublicKeyCredentialRequestOptions(
                body ?: throw ApiException(message = "Empty response from signInRequest API"),
            )
        }
    }

    /**
     * Attempts to log in a user by verifying a passkey credential with the server.
     *
     * @param publicKeyCredential The credential object from the Android Credential Manager containing
     * the signed authentication challenge.
     * @return `true` on successful login and session update, `false` on failure.
     */
    suspend fun loginWithPasskey(publicKeyCredential: PublicKeyCredential): Boolean {
        return when (val authorizationResult = authorizePasskeyWithServer(publicKeyCredential)) {
            is ApiResult.Success -> {
                authorizationResult.sessionId?.let { newSessionId ->
                    sessionId = newSessionId
                    return true
                }
                Log.e(TAG, "Passkey authorization succeeded but returned no session ID.")
                false
            }

            is ApiResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Passkey authorization failed on server")
                false
            }
        }
    }

    /**
     * Sends a public key credential to the authentication server for sign-in.
     *
     * @param publicKeyCredential: Passkey to be authorized by server
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    private suspend fun authorizePasskeyWithServer(
        publicKeyCredential: PublicKeyCredential,
    ): ApiResult<Unit> {
        val currentSessionId = sessionId ?: throw IllegalStateException(
            "Requested Passkey was not provided with a valid session.",
        )

        val signInResponseJSON = JSONObject(publicKeyCredential.authenticationResponseJson)
        val response = signInResponseJSON.getJSONObject("response")
        val credentialId = signInResponseJSON.getString("rawId")
        val httpResponse = httpClient.newCall(
            okhttp3.Request.Builder().url("$BASE_URL/webauthn/signinResponse")
                .addHeader("Cookie", formatCookie(currentSessionId))
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
        ).await()

        return httpResponse.result(errorMessage = "Error in SignIn Response") { }
    }

    /**
     * Attempts to log in a user with the provided username and password.
     *
     * This function handles the full server authentication flow. It updates the local
     * session data on success and clears it on failure.
     *
     * @param passwordCredential The object containing the user's ID and password.
     * @return `true` on successful login, `false` on failure.
     */
    suspend fun loginWithPassword(passwordCredential: PasswordCredential): Boolean {
        val usernameSessionId =
            when (val result = authorizeUsernameWithServer(passwordCredential.id)) {
                is ApiResult.Success -> {
                    result.sessionId
                }

                is ApiResult.SignedOutFromServer -> {
                    signOut()
                    Log.e(TAG, "Username ${passwordCredential.id} not found in server")
                    return false
                }
            }

        if (usernameSessionId == null) {
            signOut()
            Log.e(TAG, "Did not receive a session ID after submitting username.")
            return false
        }

        return when (
            val result =
                authorizePasswordWithServer(usernameSessionId, passwordCredential.password)
        ) {
            is ApiResult.Success -> {
                result.sessionId?.let { passwordSessionId ->
                    sessionId = passwordSessionId
                }
                true
            }

            is ApiResult.SignedOutFromServer -> {
                signOut()
                Log.e(TAG, "Password: ${passwordCredential.password} incorrect")
                sessionId = null
                false
            }
        }
    }

    /**
     * Sends a username to the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    private suspend fun authorizeUsernameWithServer(username: String): ApiResult<Unit> {
        val httpResponse = httpClient.newCall(
            Builder().url("$BASE_URL/username").method(
                "POST",
                createJSONRequestBody {
                    name(USERNAME).value(username)
                },
            ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error setting username") { }
    }

    /**
     * Sends a password to the authentication server.
     *
     * @param sessionId The session ID received from `username()`.
     * @param password A password.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    private suspend fun authorizePasswordWithServer(
        sessionId: String,
        password: String,
    ): ApiResult<Unit> {
        val httpResponse = httpClient.newCall(
            Builder().url("$BASE_URL/password").addHeader("Cookie", formatCookie(sessionId))
                .method(
                    "POST",
                    createJSONRequestBody {
                        name(PASSWORD).value(password)
                    },
                ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error setting password") { }
    }

    /**
     * Processes a custom credential, specifically handling Google ID Token credentials.
     * Authorizes the extracted Google ID token with the authentication server.
     *
     * @param credential The custom credential received from the Credential Manager.
     * @return {@code true} if the Google ID token was successfully authorized; {@code false} otherwise.
     */
    suspend fun loginWithCustomCredential(credential: CustomCredential): Boolean {
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.e(TAG, "Unrecognized custom credential: ${credential.type}")
            return false
        }
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return authorizeGoogleTokenWithServer(googleIdTokenCredential.idToken)
    }

    // Todo(johnnzoeller): Implement in next PR
    suspend fun authorizeGoogleTokenWithServer(token: String): Boolean {
        return true
    }

    fun signOut() {
        signedInState.update { false }
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
                    "challenge" -> credentialRequestOptions.put(
                        "challenge",
                        jsonReader.nextString(),
                    )

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
                    "id" -> jsonObject.put("id", b64Decode(jsonReader.nextString()))
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

    private fun b64Decode(str: String): ByteArray {
        return Base64.decode(str, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
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
                return ApiResult.SignedOutFromServer
            }
            // All other errors throw an exception.
            throwResponseError(this, errorMessage)
        }
        val cookie = headers("set-cookie").find { it.startsWith(SESSION_ID_KEY) }
        val sessionId = if (cookie != null) parseSessionId(cookie) else null
        return ApiResult.Success(sessionId, data())
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
     * Extension function for [JsonWriter] to write an object value.
     *
     * @param body A lambda expression that writes to the [JsonWriter].
     */
    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            })

            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                    Log.w(TAG, "Exception thrown while trying to cancel a Call", ex)
                }
            }
        }
    }
}
