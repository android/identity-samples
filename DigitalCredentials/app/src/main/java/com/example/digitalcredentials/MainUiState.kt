/*
 * Copyright 2026 Google LLC
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

package com.example.digitalcredentials

/**
 * Represents a single field or "claim" extracted from a digital credential.
 *
 * @property label The human-readable name of the field (e.g., "Given Name").
 * @property value The retrieved data for that field (e.g., "John").
 */
data class CredentialClaim(
    val label: String,
    val value: String
)

/**
 * Sealed interface representing the various states of the Main screen UI.
 */
sealed interface MainUiState {
    /**
     * The initial state before any request has been made.
     */
    data object Initial : MainUiState

    /**
     * Represents an active credential retrieval request in progress.
     */
    data object Loading : MainUiState

    /**
     * Represents a successful credential retrieval.
     *
     * @property title The type of credential received (e.g., "Driver's License").
     * @property claims The list of individual fields extracted from the response.
     */
    data class Success(
        val title: String,
        val claims: List<CredentialClaim>
    ) : MainUiState

    /**
     * Represents a failure in the credential retrieval flow.
     *
     * @property message A human-readable error or status message (e.g., "Request cancelled").
     */
    data class Error(val message: String) : MainUiState
}
