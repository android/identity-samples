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
package com.authentication.shrine.model

/**
 * Represents the request body for logging in with a username and password.
 *
 * @property username The username to log in with.
 * @property password The password to log in with.
 */

data class LoginUsernamePasswordRequest(
    val username: String,
    val password: String,
)

/**
 * Represents the request body for registering a new username.
 * This data class is typically used for serialization (e.g., with Gson or Kotlinx Serialization)
 * when making API calls related to new user registration.
 *
 * @property username The username to be registered.
 * @property displayName The display name for the new user.
 */
data class RegisterUsernameRequest(
    val username: String,
    val displayName: String,
)
