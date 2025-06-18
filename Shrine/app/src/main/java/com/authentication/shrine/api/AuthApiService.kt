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

import com.authentication.shrine.model.GenericAuthResponse
import com.authentication.shrine.model.PasskeysList
import com.authentication.shrine.model.PasswordRequest
import com.authentication.shrine.model.RegisterRequestRequestBody
import com.authentication.shrine.model.RegisterRequestResponse
import com.authentication.shrine.model.RegisterResponseRequestBody
import com.authentication.shrine.model.SignInRequestResponse
import com.authentication.shrine.model.SignInResponseRequest
import com.authentication.shrine.model.UsernameRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    /**
     * Sends a username to the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return A [Response] indicating the success or failure of the operation.
     */
    @POST("auth/username")
    suspend fun setUsername(
        @Body username: UsernameRequest,
    ): Response<GenericAuthResponse>

    /**
     * Sends a password to the authentication server.
     *
     * @param cookie The session ID received for the current session`.
     * @param password A password.
     * @return An [Response] indicating the success or failure of the operation.
     */
    @POST("auth/password")
    suspend fun setPassword(
        @Header("Cookie") cookie: String,
        @Body password: PasswordRequest,
    ): Response<GenericAuthResponse>

    /**
     * Requests a public key credential creation options object from the authentication server.
     *
     * @param cookie The session ID.
     * @param requestBody [RegisterRequestRequestBody] Request Body for the registerRequest API
     * @return A [Response] containing the public key credential creation options object.
     */
    @POST("webauthn/registerRequest")
    suspend fun registerRequest(
        @Header("Cookie") cookie: String,
        @Body requestBody: RegisterRequestRequestBody,
    ): Response<RegisterRequestResponse>

    /**
     * Sends a public key credential to the authentication server.
     *
     * @param cookie The session ID to be used for the sign-in.
     * @param requestBody [RegisterResponseRequestBody] Request body for the registerResponse API.
     * @return A [Response] indicating the success or failure of the operation.
     */
    @POST("webauthn/registerResponse")
    suspend fun registerResponse(
        @Header("Cookie") cookie: String,
        @Body requestBody: RegisterResponseRequestBody,
    ): Response<GenericAuthResponse>

    /**
     * Initiates the sign-in flow using passkeys.
     *
     * @return A [Response] containing the public key credential request options, or an error if the API call fails.
     */
    @POST("webauthn/signinRequest")
    suspend fun signInRequest(): Response<SignInRequestResponse>

    /**
     * Sends a public key credential to the authentication server for sign-in.
     *
     * @param cookie The session ID to be used for the sign-in.
     * @param requestBody [SignInResponseRequest] Request body for signInResponse API
     * @return A [Response] indicating the success or failure of the operation.
     */
    @POST("webauthn/signinResponse")
    suspend fun signInResponse(
        @Header("Cookie") cookie: String,
        @Body requestBody: SignInResponseRequest,
    ): Response<GenericAuthResponse>

    /**
     * Retrieves a list of passkeys associated with the current session.
     *
     * This is a suspend function that makes an asynchronous API call to fetch the passkeys.
     * It uses the provided session ID to authenticate the request.
     *
     * @param cookie The session ID used for authentication.
     * @return A [Response] object containing a [PasskeysList] on success,
     *         The [PasskeysList] contains a list of [PasskeyCredential] objects.
     *         Possible failure cases include network errors, invalid session ID,
     *         or an empty response body.
     */
    @POST("webauthn/getKeys")
    suspend fun getKeys(
        @Header("Cookie") cookie: String,
    ): Response<PasskeysList>

    /**
     * Registers a username with the authentication server.
     *
     * @param username The username to be used for sign-in.
     * @return A [Response] indicating the success or failure of the operation.
     */
    @POST("auth/new-user")
    suspend fun registerUsername(
        @Body username: UsernameRequest
    ): Response<GenericAuthResponse>
}
