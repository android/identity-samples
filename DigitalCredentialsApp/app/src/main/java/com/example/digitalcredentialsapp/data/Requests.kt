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
     * @param format The credential format (e.g., "mso_mdoc" or "dc+sd-jwt").
     * @param metaKey The key for metadata filtering (e.g., "doctype_value" or "vct_values").
     * @param metaValue The value for metadata filtering (e.g., "org.iso.18013.5.1.mDL").
     * @param requestedClaims A list of specific claims to request from the credential.
     * @return The formatted OpenID4VP JSON request string.
     */
    fun getOpenId4VpDigitalCredentialRequest(
        nonce: String,
        format: String,
        metaKey: String,
        metaValue: String,
        requestedClaims: List<RequestedClaim>
    ): String {
        val claimsJson = requestedClaims.joinToString(",") { claim ->
            """{"id": "${claim.id}", "path": ${claim.path.map { "\"$it\"" }}}"""
        }

        return """
        {
          "requests": [
            {
              "protocol": "openid4vp-v1-unsigned",
              "data": {
                "response_type": "vp_token",
                "response_mode": "dc_api",
                "nonce": "$nonce",
                "dcql_query": {
                  "credentials": [
                    {
                      "id": "digital_credential_query",
                      "format": "$format",
                      "meta": {
                        "$metaKey": ["$metaValue"]
                      },
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
