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
import retrofit2.http.Query

/**
 * Interface defining the API endpoints for authentication and WebAuthn operations.
 * This interface is intended to be used with Retrofit for making network requests.
 */
interface AuthApiService {

    /**
     * Sets or updates the username for the current session.
     *
     * @param username The request body containing the new username.
     * @return A Retrofit {@link Response} wrapping a {@link GenericAuthResponse},
     *         indicating the success or failure of the operation.
     */
    @POST("auth/username")
    suspend fun setUsername(
        @Body username: UsernameRequest,
    ): Response<GenericAuthResponse>

    /**
     * Sets or updates the password for the authenticated user.
     *
     * @param cookie The session cookie for authentication.
     * @param password The request body containing the new password information.
     * @return A Retrofit {@link Response} wrapping a {@link GenericAuthResponse},
     *         indicating the success or failure of the operation.
     */
    @POST("auth/password")
    suspend fun setPassword(
        @Header("Cookie") cookie: String,
        @Body password: PasswordRequest,
    ): Response<GenericAuthResponse>

    /**
     * Initiates a WebAuthn registration ceremony by requesting registration options
     * from the server.
     *
     * @param cookie The session cookie for authentication.
     * @param requestBody The request body, potentially containing user information
     *                    or relying party details for the registration request.
     * @return A Retrofit {@link Response} wrapping a {@link RegisterRequestResponse},
     *         which contains the challenge and options for the WebAuthn registration.
     */
    @POST("webauthn/registerRequest")
    suspend fun registerRequest(
        @Header("Cookie") cookie: String,
        @Body requestBody: RegisterRequestRequestBody,
    ): Response<RegisterRequestResponse>

    /**
     * Sends the client's response to a WebAuthn registration challenge back to the server
     * to complete the passkey registration.
     *
     * @param cookie The session cookie for authentication.
     * @param type The type of credential. Only used to specify Restore Credential passkeys to the
     * server.
     * @param requestBody The request body containing the client's attestation response
     *                    to the registration challenge.
     * @return A Retrofit {@link Response} wrapping a {@link GenericAuthResponse},
     *         indicating the success or failure of the passkey registration.
     */
    @POST("webauthn/registerResponse")
    suspend fun registerResponse(
        @Header("Cookie") cookie: String,
        @Query("type") type: String?,
        @Body requestBody: RegisterResponseRequestBody,
    ): Response<GenericAuthResponse>

    /**
     * Initiates a WebAuthn sign-in ceremony by requesting assertion options
     * (a challenge) from the server.
     *
     * @return A Retrofit {@link Response} wrapping a {@link SignInRequestResponse},
     *         which contains the challenge and options for the WebAuthn sign-in.
     */
    @POST("webauthn/signinRequest")
    suspend fun signInRequest(): Response<SignInRequestResponse>

    /**
     * Sends the client's response to a WebAuthn sign-in challenge (assertion) back
     * to the server to complete the authentication.
     *
     * @param cookie The session cookie that might have been established or is being verified.
     * @param requestBody The request body containing the client's assertion response
     *                    to the sign-in challenge.
     * @return A Retrofit {@link Response} wrapping a {@link GenericAuthResponse},
     *         indicating the success or failure of the WebAuthn sign-in.
     */
    @POST("webauthn/signinResponse")
    suspend fun signInResponse(
        @Header("Cookie") cookie: String,
        @Body requestBody: SignInResponseRequest,
    ): Response<GenericAuthResponse>

    /**
     * Retrieves a list of registered passkeys (WebAuthn credentials) for the
     * authenticated user.
     *
     * @param cookie The session cookie for authentication.
     * @return A Retrofit {@link Response} wrapping a {@link PasskeysList},
     *         containing the list of the user's passkeys.
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
        @Body username: UsernameRequest,
    ): Response<GenericAuthResponse>

    /**
     * Deletes a passkey from the authentication server.
     */
    @POST("webauthn/removeKey")
    suspend fun deletePasskey(
        @Header("Cookie") cookie: String,
        @Query("credId") credentialId: String,
    ): Response<GenericAuthResponse>
}
