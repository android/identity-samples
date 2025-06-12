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
package com.authentication.shrinewear

import com.authentication.shrinewear.api.*
import android.util.Base64
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit
import android.os.Build
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType.PUBLIC_KEY
import android.util.JsonReader
import android.util.JsonToken.STRING
import android.util.JsonWriter
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request.Builder
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringWriter
import java.io.IOException
import java.io.StringReader


const val SERVER_CLIENT_ID = BuildConfig.CLIENT_SECRET

data class CredentialData(
    var username: String? = null,
    var sessionId: String? = null,
    var isPassKeyAuthenticated: Boolean? = false,
)

class AuthenticationServer {
    private val signedInState = MutableStateFlow(false)
    private val httpClient: OkHttpClient
    private val credentialData: CredentialData = CredentialData()

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

    init {
        val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
                "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"
        httpClient = OkHttpClient.Builder()
            .addInterceptor(AddHeaderInterceptor(userAgent))
            .addInterceptor(HttpLoggingInterceptor { message ->
                println("LOG-APP: $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Todo
     */
    suspend fun getPublicKeyRequestOptions(): /*JSONObject?*/ String {
        when (val publicKeyRequestOptions = retrievePublicKeyRequestOptions()) {
            // Passkey data could not be retrieved... does not necessarily kill the request.
            // Deciding what kind of value to return here depends on the http server behavior
            is ApiResult.SignedOutFromServer -> {
                signOut()
                return ""
            }
            // Looking at the server, i'm 90% sure the non-error results will always contain
            // *something* in the string..
            is ApiResult.Success -> {
                credentialData.sessionId = publicKeyRequestOptions.sessionId!!
                return publicKeyRequestOptions.data.toString()
            }
        }
    }

    /**
     * Retrieves passkeys public key request options, if they exist.
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
                body ?:
                throw ApiException(message = "Empty response from signInRequest API"),
            )
        }
    }

    suspend fun loginWithPasskey(publicKeyCredential: PublicKeyCredential): Boolean {
        val signInResponseJSON = JSONObject(publicKeyCredential.authenticationResponseJson)
        val signInResult = signInWithPasskeysResponse(
            credentialData.sessionId!!,
            signInResponseJSON.getJSONObject("response"),
            signInResponseJSON.getString("rawId")
        )

        if (signInResult is ApiResult.Success) {
            credentialData.sessionId = signInResult.sessionId!!
            return true
        } else {
            signOut()
            return false
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
    private suspend fun signInWithPasskeysResponse(
        sessionId: String,
        response: JSONObject,
        credentialId: String,
    ): ApiResult<Unit> {
        val httpResponse = httpClient.newCall(
            okhttp3.Request.Builder().url("$BASE_URL/webauthn/signinResponse")
                .addHeader("Cookie", formatCookie(sessionId))
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

        Log.e("john", "here2")

        return httpResponse.result(errorMessage = "Error in SignIn Response") { }
    }

    suspend fun loginWithPassword(passwordCredential: PasswordCredential): Boolean {
        return when (val result = setUsername(passwordCredential.id)) {
            ApiResult.SignedOutFromServer -> {
                Log.e(TAG, "failed password")
                signOut()
                false
            }
            is ApiResult.Success -> {
                Log.e(TAG, "good password")
                credentialData.username = passwordCredential.id
                credentialData.sessionId = result.sessionId!!
                setSessionWithPassword(passwordCredential.password)
                true
            }
        }
    }

    /**
     * Sends a username to the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    private suspend fun setUsername(username: String): ApiResult<Unit> {
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
     * Signs in with a password.
     *
     * @param password The password to use.
     * @return True if the sign-in was successful, false otherwise.
     */
    private suspend fun setSessionWithPassword(password: String): Boolean {
        val username = credentialData.username
        val sessionId = credentialData.sessionId
        if (!username.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
            try {
                return when (val result = setPassword(sessionId, password)) {
                    ApiResult.SignedOutFromServer -> {
                        signOut()
                        false
                    }

                    is ApiResult.Success<*> -> {
                        if (result.sessionId != null) {
                            credentialData.sessionId = result.sessionId
                        }
                        true
                    }
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Invalid login credentials", e)
                credentialData.username = null
                credentialData.sessionId = null
            }
        } else {
            Log.e(TAG, "Please check if username and session id is present in your datastore")
        }
        return false
    }

    /**
     * Sends a password to the authentication server.
     *
     * @param sessionId The session ID received from `username()`.
     * @param password A password.
     * @return An [ApiResult] indicating the success or failure of the operation.
     */
    private suspend fun setPassword(sessionId: String, password: String): ApiResult<Unit> {
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

    suspend fun loginWithGoogleToken(token: String): Boolean {
        return true
//        return suspendCoroutine { continuation ->
//            val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
//            firebaseAuth.signInWithCredential(firebaseCredential).addOnSuccessListener {
//                continuation.resume(true)
//            }.addOnFailureListener {
//                Log.e("TAG", "login with google id failed: $it")
//                continuation.resume(false)
//            }
//        }
    }

    fun createGetGoogleIdOption(autoSelect: Boolean = false): GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SERVER_CLIENT_ID)
            .setAutoSelectEnabled(autoSelect)
            .build()

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
                        jsonReader.nextString()
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

    private fun b64Encode(str: ByteArray): String {
        return Base64.encodeToString(str, Base64.NO_WRAP or Base64.URL_SAFE)
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

    fun signOut() {
        signedInState.update { false }
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            })

            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                    // Ignore cancel exception
                    // Maybe I should throw an exception ...
                }
            }
        }
    }

}
