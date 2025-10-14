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
package com.authentication.shrinewear.network

import android.os.Build
import com.authentication.shrinewear.BuildConfig
import com.authentication.shrinewear.extensions.await
import com.authentication.shrinewear.extensions.createJSONRequestBody
import com.authentication.shrinewear.extensions.createPasskeyValidationRequest
import com.authentication.shrinewear.extensions.parsePublicKeyCredentialRequestOptions
import com.authentication.shrinewear.extensions.result
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthNetworkClient {
    private val httpClient: OkHttpClient

    companion object {
        /**
         * The base URL of the authentication server.
         */
        private const val BASE_URL = BuildConfig.API_BASE_URL

        /**
         * The key used for the session ID cookie.
         */
        internal const val SESSION_ID_KEY = "SESAME_SESSION_COOKIE="
    }

    init {
        val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"
        httpClient = OkHttpClient.Builder()
            .addInterceptor(NetworkAddHeaderInterceptor(userAgent))
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
     * Retrieves passkeys public key request options from the auth server, if they exist.
     *
     * @return An [NetworkResult] containing the public key
     * credential request options, or an error if the API call fails.
     */
    internal suspend fun fetchPublicKeyRequestOptions(): NetworkResult<JSONObject> {
        val httpResponse = httpClient.newCall(
            Request.Builder().url(
                buildString { append("$BASE_URL/webauthn/signinRequest") },
            ).method("POST", createJSONRequestBody {}).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error in SignIn with Passkeys Request") {
            parsePublicKeyCredentialRequestOptions(
                body ?: throw NetworkException(message = "Empty response from signInRequest API"),
            )
        }
    }

    /**
     * Sends a public key credential to the authentication server for sign-in.
     *
     * @param signedPasskeyData: Passkey data needed for server authentication
     * @return An [NetworkResult] indicating the success or failure of the operation.
     */
    internal suspend fun authorizePasskeyWithServer(
        passkeyResponseJSON: String,
        sessionId: String? = null,
    ): NetworkResult<Unit> {
        val currentSessionId = sessionId
            ?: throw IllegalStateException("Requested Passkey was not provided with a valid session.")

        val httpResponse = httpClient.newCall(
            Request.Builder().url("${BASE_URL}/webauthn/signinResponse")
                .addHeader("Cookie", "$SESSION_ID_KEY$currentSessionId")
                .method(
                    "POST",
                    createPasskeyValidationRequest(passkeyResponseJSON),
                ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error in SignIn Response") { }
    }

    /**
     * Sends a username to the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return An [NetworkResult] indicating the success or failure of the operation.
     */
    internal suspend fun authorizeUsernameWithServer(username: String): NetworkResult<Unit> {
        val httpResponse = httpClient.newCall(
            Request.Builder().url("${BASE_URL}/auth/username").method(
                "POST",
                createJSONRequestBody {
                    name("username").value(username)
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
     * @return An [NetworkResult] indicating the success or failure of the operation.
     */
    internal suspend fun authorizePasswordWithServer(
        sessionId: String,
        password: String,
    ): NetworkResult<Unit> {
        val httpResponse = httpClient.newCall(
            Request.Builder().url("${BASE_URL}/auth/password")
                .addHeader("Cookie", "$SESSION_ID_KEY$sessionId")
                .method(
                    "POST",
                    createJSONRequestBody {
                        name("password").value(password)
                    },
                ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error setting password") { }
    }

    /**
     * Fetches a session ID from the backend server to enable ID token validation.
     *
     * @return [NetworkResult.Success] with the session ID, or an [NetworkResult.Error]/[NetworkResult.SignedOutFromServer] on failure.
     */
    internal suspend fun fetchFederationOptions(): NetworkResult<Unit> {
        val httpResponse = httpClient.newCall(
            Request.Builder().url("${BASE_URL}/federation/options").method(
                "POST",
                createJSONRequestBody {
                    name("urls").beginArray().value("https://accounts.google.com").endArray()
                },
            ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error creating federation options") {}
    }

    /**
     * Authorizes a federated identity token with the backend server.
     *
     * This function sends a POST request to the server's `/federation/verifyIdToken` endpoint,
     * passing the federated ID token and desired accounts URLs for verification.
     *
     * @param token The ID token obtained from the Sign-In.
     * @return [NetworkResult]<[Unit]> indicating the success or failure of the authorization.
     * A [Unit] type for success implies no specific data is returned on successful authorization.
     */
    internal suspend fun authorizeFederatedTokenWithServer(
        token: String,
        sessionId: String,
    ): NetworkResult<Unit> {
        val requestHeaders = okhttp3.Headers.Builder().add(
            "Cookie",
            "$SESSION_ID_KEY$sessionId",
        ).build()

        val httpResponse = httpClient.newCall(
            Request.Builder().url("${BASE_URL}/federation/verifyIdToken")
                .headers(requestHeaders)
                .method(
                    "POST",
                    createJSONRequestBody {
                        name("token").value(token)
                        name("url").value("https://accounts.google.com")
                    },
                ).build(),
        ).await()

        return httpResponse.result(errorMessage = "Error signing in with the federated token") { }
    }
}
