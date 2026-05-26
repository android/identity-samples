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
 * Represents a claim being requested in a Digital Credential query.
 *
 * @property id A unique identifier for this claim within the query.
 * @property path The path to the claim in the credential's namespace (e.g., ["org.iso.18013.5.1", "family_name"]).
 */
data class RequestedClaim(
    val id: String,
    val path: List<String>
)

/**
 * Represents JSON request strings for the Digital Credentials API.
 *
 * This object follows the OpenID4VP specifications to request credentials from digital wallets.
 */
object Requests {

    /**
     * Generates a dynamic OpenID4VP JSON request for any digital credential.
     *
     * @param nonce A cryptographically secure random string to prevent replay attacks.
     * @param protocol The protocol identifier (e.g., "openid4vp-v1" or "openid4vp-v1-unsigned").
     * @param clientId The unique identifier for the verifier (optional).
     * @param clientMetadata Optional JSON string for client_metadata (e.g., supported algorithms).
     * @param format The credential format (e.g., "mso_mdoc" or "dc+sd-jwt").
     * @param meta The metadata object for filtering (passed as a raw JSON string).
     * @param requestedClaims A list of specific claims to request from the credential.
     * @return The formatted OpenID4VP JSON request string.
     */
    fun getOpenId4VpDigitalCredentialRequest(
        nonce: String,
        protocol: String,
        clientId: String?,
        clientMetadata: String?,
        format: String,
        meta: String,
        requestedClaims: List<RequestedClaim>
    ): String {
        val claimsJson = requestedClaims.joinToString(",") { claim ->
            """{"path": ${claim.path.map { "\"$it\"" }}}"""
        }

        val clientIdJson = if (clientId != null) """ "client_id": "$clientId", """ else ""
        val clientMetadataJson = if (clientMetadata != null) """ "client_metadata": $clientMetadata, """ else ""

        return """
        {
          "requests": [
            {
              "protocol": "$protocol",
              "data": {
                "response_type": "vp_token",
                "response_mode": "dc_api",
                $clientIdJson
                $clientMetadataJson
                "nonce": "$nonce",
                "dcql_query": {
                  "credentials": [
                    {
                      "id": "cred1",
                      "format": "$format",
                      "meta": $meta,
                      "claims": [$claimsJson]
                    }
                  ]
                }
              }
            }
          ]
        }
        """.trimIndent()
    }
}
