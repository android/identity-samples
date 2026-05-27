package com.example.digitalcredentialsapp

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.example.digitalcredentialsapp.data.CborTag
import com.example.digitalcredentialsapp.data.cborDecode
import com.example.digitalcredentialsapp.data.RequestedClaim
import com.example.digitalcredentialsapp.data.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import kotlin.collections.get

/**
 * Utility object for interacting with the Android Credential Manager API.
 *
 * This object handles the creation of credential requests and the parsing of responses
 * for both Mobile Driver's Licenses (mDL) and Verified Emails.
 */
object CredentialManagerUtil {

    /**
     * Initiates a request for a Digital Credential (e.g., mDL).
     *
     * @param activity The activity context used to display the Credential Manager UI.
     * @return The resulting [MainUiState].
     */
    suspend fun getDigitalCredential(activity: Activity): MainUiState {
        val requestJson = simulateMdlServerResponse()
        return requestCredential(activity, requestJson)
    }

    /**
     * Initiates a request for a Verified Email credential.
     *
     * @param activity The activity context used to display the Credential Manager UI.
     * @return The resulting [MainUiState].
     */
    suspend fun getVerifiedEmailCredential(activity: Activity): MainUiState {
        val requestJson = simulateEmailServerResponse()
        return requestCredential(activity, requestJson)
    }

    /**
     * Simulates a server-side request generation for an mDL.
     */
    private suspend fun simulateMdlServerResponse(): String = withContext(Dispatchers.IO) {
        Requests.getOpenId4VpDigitalCredentialRequest(
            nonce = generateSecureRandomNonce(),
            protocol = "openid4vp-v1-unsigned",
            clientId = null,
            clientMetadata = """
                {
                  "vp_formats_supported": {
                    "mso_mdoc": {
                      "deviceauth_alg_values": [-7],
                      "issuerauth_alg_values": [-7]
                    }
                  }
                }
            """.trimIndent(),
            format = "mso_mdoc",
            meta = """{"doctype_value": "org.iso.18013.5.1.mDL"}""",
            requestedClaims = listOf(
                RequestedClaim("", listOf("org.iso.18013.5.1", "family_name")),
                RequestedClaim("", listOf("org.iso.18013.5.1", "given_name")),
                RequestedClaim("", listOf("org.iso.18013.5.1", "age_over_21"))
            )
        )
    }

    /**
     * Simulates a server-side request generation for a Verified Email.
     */
    private suspend fun simulateEmailServerResponse(): String = withContext(Dispatchers.IO) {
        Requests.getOpenId4VpDigitalCredentialRequest(
            nonce = generateSecureRandomNonce(),
            protocol = "openid4vp-v1-unsigned",
            clientId = null,
            clientMetadata = null,
            format = "dc+sd-jwt",
            meta = """{"vct_values": ["UserInfoCredential"]}""",
            requestedClaims = listOf(
                RequestedClaim("email", listOf("email")),
                RequestedClaim("email_verified", listOf("email_verified"))
            )
        )
    }

    /**
     * Internal helper to execute a credential request and handle common error states.
     */
    @OptIn(ExperimentalDigitalCredentialApi::class)
    private suspend fun requestCredential(
        activity: Activity,
        requestJson: String
    ): MainUiState = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(activity)

            val getDigitalCredentialOption = GetDigitalCredentialOption(requestJson = requestJson)
            val request = GetCredentialRequest(listOf(getDigitalCredentialOption))

            val result = credentialManager.getCredential(activity, request)
            verifyResult(result)
        } catch (e: GetCredentialException) {
            handleFailure(e)
        } catch (e: Exception) {
            Log.e("CredentialManagerUtil", "Unexpected error", e)
            MainUiState.Error(e.message ?: "An unknown error occurred")
        }
    }

    /**
     * Handles the successfully returned credential.
     */
    @OptIn(ExperimentalDigitalCredentialApi::class)
    private fun verifyResult(result: GetCredentialResponse): MainUiState {
        return when (val credential = result.credential) {
            is DigitalCredential -> {
                val responseJson = credential.credentialJson
                validateResponseOnServer(responseJson)
                val claims = parseClaims(responseJson)
                MainUiState.Success(
                    title = "Credential Received",
                    claims = claims
                )
            }
            else -> {
                Log.e("CredentialManagerUtil", "Unexpected type of credential ${credential.type}")
                MainUiState.Error("Unexpected credential type: ${credential.type}")
            }
        }
    }

    /**
     * Handles credential request failures.
     */
    private fun handleFailure(e: GetCredentialException): MainUiState {
        return when (e) {
            is GetCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to share the credential.
                MainUiState.Error("Request was cancelled")
            }
            is GetCredentialInterruptedException -> {
                // Retry-able error. Consider retrying the call.
                MainUiState.Error("Request was interrupted. Please try again.")
            }
            is NoCredentialException -> {
                // No credential was available.
                MainUiState.Error("No matching credentials found")
            }
            is GetCredentialUnknownException -> {
                // An unknown, usually unexpected, error has occurred. Check the
                // message error for any additional debugging information.
                MainUiState.Error("An unknown error occurred: ${e.message}")
            }
            is GetCredentialCustomException -> {
                // You have encountered a custom error thrown by the wallet.
                MainUiState.Error("A custom error occurred: ${e.type}")
            }
            else -> {
                Log.w("CredentialManagerUtil", "Unexpected exception type ${e::class.java}")
                MainUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Simulates sending the response to a backend server for validation.
     *
     * @param responseJson The raw JSON response from the Digital Credentials API.
     */
    private fun validateResponseOnServer(responseJson: String) {
        Log.d("CredentialManagerUtil", "Response validated on (simulated) server")
    }

    /**
     * Generates a cryptographically secure nonce for the request.
     */
    private fun generateSecureRandomNonce(): String {
        val sr = SecureRandom()
        val nonceBytes = ByteArray(32)
        sr.nextBytes(nonceBytes)
        return Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Parses the raw JSON response from the Digital Credentials API into a list of [CredentialClaim]s.
     *
     * In DCQL (Digital Credential Query Language), the response contains a 'vp_token'
     * which can be a single token string or a map of credential IDs to tokens.
     */
    private fun parseClaims(responseJsonString: String): List<CredentialClaim> {
        val claims = mutableListOf<CredentialClaim>()
        try {
            val responseJson = JSONObject(responseJsonString)
            val data = responseJson.optJSONObject("data") ?: responseJson
            val vpToken = data.opt("vp_token") ?: return emptyList()

            when (vpToken) {
                is JSONObject -> {
                    // Handle map of IDs to tokens/arrays
                    val keys = vpToken.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val token = when (val value = vpToken.get(key)) {
                            is JSONArray -> if (value.length() > 0) value.getString(0) else null
                            is String -> value
                            else -> null
                        }
                        token?.let { claims.addAll(parseToken(it)) }
                    }
                }
                is JSONArray -> {
                    // Handle array of tokens
                    for (i in 0 until vpToken.length()) {
                        val token = vpToken.optString(i)
                        if (token.isNotEmpty()) claims.addAll(parseToken(token))
                    }
                }
                is String -> {
                    // Handle single token string
                    claims.addAll(parseToken(vpToken))
                }
            }
        } catch (e: Exception) {
            Log.e("CredentialManagerUtil", "Failed to parse claims", e)
        }
        return claims
    }

    /**
     * Identifies the format of a raw token and parses its claims.
     */
    private fun parseToken(rawToken: String): List<CredentialClaim> {
        return if (rawToken.contains("~")) {
            parseSdJwtClaims(rawToken)
        } else {
            parseMdocClaims(rawToken)
        }
    }

    /**
     * Specifically parses claims from an mDoc (ISO 18013-5) binary response.
     */
    private fun parseMdocClaims(base64Mdoc: String): List<CredentialClaim> {
        val claims = mutableListOf<CredentialClaim>()
        try {
            val bytes = Base64.decode(base64Mdoc, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val decoded = cborDecode(bytes) as? Map<*, *> ?: return emptyList()

            val documents = decoded["documents"] as? List<*> ?: return emptyList()
            for (doc in documents) {
                val docMap = doc as? Map<*, *> ?: continue
                val issuerSigned = docMap["issuerSigned"] as? Map<*, *> ?: continue
                val nameSpaces = issuerSigned["nameSpaces"] as? Map<*, *> ?: continue

                val mdlNamespace = nameSpaces["org.iso.18013.5.1"]

                if (mdlNamespace is List<*>) {
                    for (item in mdlNamespace) {
                        val decodedItem = if (item is CborTag && item.tag == 24L) {
                            cborDecode(item.item as ByteArray)
                        } else {
                            item
                        }
                        (decodedItem as? Map<*, *>)?.let { extractClaim(it, claims) }
                    }
                } else if (mdlNamespace is Map<*, *>) {
                    mdlNamespace.forEach { (k, v) ->
                        val value = if (v is CborTag && v.tag == 24L) {
                            cborDecode(v.item as ByteArray)
                        } else {
                            v
                        }
                        claims.add(CredentialClaim(formatLabel(k.toString()), value.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CredentialManagerUtil", "Failed to parse mDoc", e)
        }
        return claims
    }

    /**
     * Extracts an individual claim from an IssuerSignedItem map.
     */
    private fun extractClaim(itemMap: Map<*, *>, claims: MutableList<CredentialClaim>) {
        val key = itemMap["elementIdentifier"] as? String ?: return
        val value = itemMap["elementValue"] ?: return

        val formattedValue = when (value) {
            is Boolean -> if (value) "Yes" else "No"
            else -> value.toString()
        }
        claims.add(CredentialClaim(formatLabel(key), formattedValue))
    }

    /**
     * Parses claims from an SD-JWT (Selective Disclosure JWT) format.
     */
    private fun parseSdJwtClaims(sdJwt: String): List<CredentialClaim> {
        val claims = mutableListOf<CredentialClaim>()
        val parts = sdJwt.split("~")
        for (i in 1 until parts.size) {
            val part = parts[i]
            if (part.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP)
                    val jsonArray = JSONArray(String(decodedBytes))
                    if (jsonArray.length() == 3) {
                        val key = jsonArray.getString(1)
                        val value = jsonArray.get(2).toString()
                        claims.add(CredentialClaim(formatLabel(key), value))
                    }
                } catch (e: Exception) {
                    // Skip invalid parts
                }
            }
        }
        return claims
    }

    /**
     * Formats a raw identifier key into a human-readable label.
     */
    private fun formatLabel(key: String): String {
        return key.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}