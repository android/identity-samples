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

package com.example.digitalcredentialsapp.data

/**
 * Represents the OpenID4VP Presentation Submission metadata.
 *
 * This structure follows the DIF Presentation Exchange (PE) specification,
 * identifying which requested credentials were provided and where they reside.
 *
 * @property id Unique identifier for this submission.
 * @property definition_id The ID of the Presentation Definition this submission satisfies.
 * @property descriptor_map A list of mappings from credential IDs to their location in the token.
 */
data class PresentationSubmission(
    val id: String,
    val definition_id: String,
    val descriptor_map: List<DescriptorMapping>
)

/**
 * Maps a specific credential in the response to its format and path.
 *
 * @property id The identifier of the input descriptor from the request.
 * @property format The format of the credential (e.g., "mso_mdoc" or "dc+sd-jwt").
 * @property path A JSONPath pointing to the credential within the vp_token.
 */
data class DescriptorMapping(
    val id: String,
    val format: String,
    val path: String
)
