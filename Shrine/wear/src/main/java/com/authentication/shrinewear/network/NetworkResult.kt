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

/**
 * Represents the result of an API call.
 *
 * This sealed class has three subclasses:
 * - [Success]: The API call returned successfully with data.
 * - [SignedOutFromServer]: The API call returned unsuccessfully with code 401, and the user should be signed out.
 */
sealed class NetworkResult<out R> {

    /**
     * API returned successfully with data.
     *
     * @param sessionId The session ID to be used for the subsequent API calls.
     * Might be null if the API call does not return a new cookie.
     * @param data The result data.
     */
    class Success<T>(
        val sessionId: String?,
        val data: T,
    ) : NetworkResult<T>()

    /**
     * API returned unsuccessfully with code 401, and the user should be considered signed out.
     */
    data object SignedOutFromServer : NetworkResult<Nothing>()
}
