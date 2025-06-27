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
 * Represents a generic authentication response from the server.
 * This data class is typically used as a common response structure for various
 * authentication-related operations, such as setting a username, password,
 * or completing certain WebAuthn steps.
 *
 * It usually contains basic information about the authenticated user or the
 * outcome of an authentication operation.
 *
 * @property id The unique identifier for the user or session, often a UUID or a server-generated ID.
 * @property username The username associated with the authenticated account.
 * @property displayName The display name for the user, which might be the same as the username
 *                       or a more user-friendly name.
 */
data class GenericAuthResponse(
    val id: String,
    val username: String,
    val displayName: String,
)
