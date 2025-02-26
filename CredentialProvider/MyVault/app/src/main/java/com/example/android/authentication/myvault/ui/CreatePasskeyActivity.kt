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
package com.example.android.authentication.myvault.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.SigningInfo
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo.Builder
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BiometricPromptResult
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.fragment.app.FragmentActivity
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasskeyMetadata
import com.example.android.authentication.myvault.fido.AssetLinkVerifier
import com.example.android.authentication.myvault.fido.AuthenticatorAttestationResponse
import com.example.android.authentication.myvault.fido.Cbor
import com.example.android.authentication.myvault.fido.FidoPublicKeyCredential
import com.example.android.authentication.myvault.fido.PublicKeyCredentialCreationOptions
import com.example.android.authentication.myvault.fido.appInfoToOrigin
import com.example.android.authentication.myvault.fido.b64Encode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.net.URL
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant

/**
 * This class is responsible for handling the public key credential (Passkey) creation request from a Relying Party i.e calling app
 */
class CreatePasskeyActivity : FragmentActivity() {
    private val credentialsDataSource = AppDependencies.credentialsDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        if (request == null) {
            // If the request is null, send an unknown exception to client and finish the flow
            setUpFailureResponseAndFinish("Unable to extract request from intent")
            return
        }

        handleCreatePublicKeyCredentialRequest(request)
    }

    /**
     * If the request is null, send an unknown exception to client and finish the flow.
     *
     * @param message The error message to send to the client.
     */
    private fun setUpFailureResponseAndFinish(message: String) {
        val result = Intent()
        PendingIntentHandler.setGetCredentialException(
            result,
            GetCredentialUnknownException(message),
        )
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    /**
     * This method handle public key credential creation request from client app
     *
     * @param request : Final request received by the provider after the user has selected a given CreateEntry on the UI.
     */
    private fun handleCreatePublicKeyCredentialRequest(request: ProviderCreateCredentialRequest) {
        val accountId = intent.getStringExtra(KEY_ACCOUNT_ID)

        // Retrieve the biometric prompt result from the request.
        val biometricPromptResult = request.biometricPromptResult

        // Check if there was an error during the biometric flow. If so, handle the error and return.
        if (isValidBiometricFlowError(biometricPromptResult)) return

        // access the associated intent and pass it into the PendingIntentHandler class to get the ProviderCreateCredentialRequest.
        if (request.callingRequest is CreatePublicKeyCredentialRequest) {
            val publicKeyRequest: CreatePublicKeyCredentialRequest =
                request.callingRequest as CreatePublicKeyCredentialRequest

            // Check if the biometric prompt result contains a successful authentication result.
            if (biometricPromptResult?.authenticationResult != null) {
                // If biometric authentication was successful, use the biometric flow to create the passkey.
                createPasskeyWithBiometricFlow(
                    publicKeyRequest.requestJson,
                    request.callingAppInfo,
                    publicKeyRequest.clientDataHash,
                    accountId,
                )
                return
            }

            // If biometric authentication was not used or was not successful, use the default flow.
            createPasskeyWithDefaultFlow(
                publicKeyRequest.requestJson,
                request.callingAppInfo,
                publicKeyRequest.clientDataHash,
                accountId,
            )
        } else {
            setUpFailureResponseAndFinish(getString(R.string.unexpected_create_request_found_in_intent))
            return
        }
    }

    /**
     * Checks if there was an error during the biometric authentication flow.
     *
     * This method determines whether the biometric authentication flow resulted in
     * an error. It checks if the {@link BiometricPromptResult} is null or if the
     * authentication was successful. If neither of these conditions is met, it
     * extracts the error code and message from the {@link BiometricPromptResult}
     * and sets up a failure response to be sent to the client.
     *
     * @param biometricPromptResult The result of the biometric authentication prompt.
     * @return True if there was an error during the biometric flow, false otherwise.
     */
    @SuppressLint("StringFormatMatches")
    private fun isValidBiometricFlowError(biometricPromptResult: BiometricPromptResult?): Boolean {
        // If the biometricPromptResult is null, there was no error.
        if (biometricPromptResult == null) return false
        if (biometricPromptResult.isSuccessful) return false

        // Initialize default values for the error code and message.
        var biometricAuthErrorCode = -1
        var biometricAuthErrorMsg = getString(R.string.unknown_failure)

        // Check if there is an authentication error in the biometricPromptResult.
        if (biometricPromptResult.authenticationError != null) {
            biometricAuthErrorCode = biometricPromptResult.authenticationError!!.errorCode
            biometricAuthErrorMsg = biometricPromptResult.authenticationError!!.errorMsg.toString()
        }

        // Build the error message to be sent to the client.
        setUpFailureResponseAndFinish(
            buildString {
                append(
                    getString(
                        R.string.biometric_error_code_with_message,
                        biometricAuthErrorCode,
                    ),
                )
                append(biometricAuthErrorMsg)
                append(getString(R.string.other_providers_error_message))
            },
        )
        return true
    }

    /**
     * This method validates the digital asset linking to verify the app identity,
     * surface biometric prompt and sends back response to client app for passkey created
     *
     * @param requestJson the request in JSON format in the [standard webauthn web json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
     * @param clientDataHash a clientDataHash value to sign over in place of assembling and hashing
     * @param clientDataJSON during the signature request; only meaningful when [origin] is set
     * @param clientDataHash a clientDataHash value to sign over in place of assembling and hashing
     */
    private fun createPasskeyWithDefaultFlow(
        requestJson: String,
        callingAppInfo: CallingAppInfo?,
        clientDataHash: ByteArray?,
        accountId: String?,
    ) {
        if (callingAppInfo == null) {
            finish()
            return
        }

        val request = PublicKeyCredentialCreationOptions(requestJson)

        if (!hasRequestContainsOrigin(callingAppInfo)) {
            // Native call. Check for asset links
            validateAssetLinks(request.rp.id, callingAppInfo)
        }

        // Surface an authentication prompt. The example below uses the Android Biometric API.
        val biometricPrompt = BiometricPrompt(
            this,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, getString(R.string.authentication_error, errString))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e(TAG, getString(R.string.authentication_failed))
                    finish()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)

                    createPasskeyWithBiometricFlow(
                        requestJson,
                        callingAppInfo,
                        clientDataHash,
                        accountId,
                    )
                }
            },
        )
        authenticate(biometricPrompt)
    }

    /**
     * Creates a passkey with own biometric flow on Android 15 & higher.
     *
     * This method handles the process of creating a passkey, including validating
     * the calling application, generating a credential ID and key pair, saving
     * the passkey, updating metadata, constructing a WebAuthn response, and setting
     * the intent for the credential response.
     *
     * @param requestJson The JSON string representing the public key credential
     * creation options.
     * @param callingAppInfo Information about the calling application. If null, the
     * method will finish execution.
     * @param clientDataHash A hash of the client data.
     * @param accountId The ID of the account.
     * @throws IllegalArgumentException if the calling app is not privileged and
     * asset links validation fails.
     */
    private fun createPasskeyWithBiometricFlow(
        requestJson: String,
        callingAppInfo: CallingAppInfo?,
        clientDataHash: ByteArray?,
        accountId: String?,
    ) {
        if (callingAppInfo == null) {
            finish()
            return
        }

        val request = PublicKeyCredentialCreationOptions(requestJson)

        var callingAppInfoOrigin: String? = null
        if (hasRequestContainsOrigin(callingAppInfo)) {
            callingAppInfoOrigin = validatePrivilegedCallingApp(
                callingAppInfo,
            ) ?: return
        } else {
            // Native call. Check for asset links
            validateAssetLinks(request.rp.id, callingAppInfo)
        }

        // Generate CredentialID
        val credentialId = ByteArray(32)
        SecureRandom().nextBytes(credentialId)

        // Generate key
        val keyPair = generateKeyPair()

        // Save the private key in your local database against callingAppInfo.packageName.
        savePasskeyInCredentialsDataStore(request, credentialId, keyPair)

        updateMetaInSharedPreferences(accountId)

        var callingOrigin = appInfoToOrigin(callingAppInfo)
        if (callingAppInfoOrigin != null) {
            callingOrigin = callingAppInfoOrigin
        }
        val response = constructWebAuthnResponse(
            keyPair,
            request,
            credentialId,
            callingOrigin,
            callingAppInfo,
            clientDataHash,
        )

        setIntentForCredentialCredentialResponse(credentialId, response)
    }

    /**
     * This method constructs a WebAuthn response object.
     *
     * @param keyPair The key pair used to generate the credential.
     * @param request The PublicKeyCredentialCreationOptions object containing the client's request.
     * @param credId The credential ID.
     * @param callingOrigin The origin of the calling application.
     * @param callingAppInfo Information about the calling application.
     * @param clientDataHash The client data hash.
     * @return An AuthenticatorAttestationResponse object.
     */
    private fun constructWebAuthnResponse(
        keyPair: KeyPair,
        request: PublicKeyCredentialCreationOptions,
        credId: ByteArray,
        callingOrigin: String,
        callingAppInfo: CallingAppInfo,
        clientDataHash: ByteArray?,
    ): AuthenticatorAttestationResponse {
        val coseKey = publicKeyToCose(keyPair.public as ECPublicKey)
        val spki = coseKeyToSPKI(coseKey)

        // Construct a Web Authentication API JSON response that consists of the public key and the credentialId.
        val response = AuthenticatorAttestationResponse(
            requestOptions = request,
            credentialId = credId,
            credentialPublicKey = Cbor().encode(coseKey),
            origin = callingOrigin,
            up = true,
            uv = true,
            be = true,
            bs = true,
            packageName = callingAppInfo.packageName,
            clientDataHash = clientDataHash,
            spki,
        )
        return response
    }

    /**
     * Generates a new key pair for use in creating a public key credential.
     *
     * @return A new [KeyPair] instance.
     */
    private fun generateKeyPair(): KeyPair {
        val spec = ECGenParameterSpec(getString(R.string.secp_256_r1))
        val keyPairGen = KeyPairGenerator.getInstance(getString(R.string.ec))
        keyPairGen.initialize(spec)
        return keyPairGen.genKeyPair()
    }

    /**
     *  This method helps check the asset linking to verify client app idenity
     * @param rpId : Relying party identifier
     * @param callingAppInfo : Information pertaining to the calling application.
     */
    private fun validateAssetLinks(rpId: String, callingAppInfo: CallingAppInfo) {
        val isRpValid: Boolean = runBlocking {
            val isRpValidDeferred: Deferred<Boolean> = async(Dispatchers.IO) {
                if (!isValidRpId(
                        rpId,
                        callingAppInfo.signingInfo,
                        callingAppInfo.packageName,
                    )
                ) {
                    return@async false
                }
                return@async true
            }
            return@runBlocking isRpValidDeferred.await()
        }

        if (!isRpValid) {
            setUpFailureResponseAndFinish(getString(R.string.failed_to_validate_rp))
            return
        }
    }

    /**
     * Checks if the given Relying Party (RP) identifier is valid.
     *
     * @param rpId The RP identifier to validate.
     * @param signingInfo The signing information of the calling application.
     * @param callingPackage The package name of the calling application.
     * @return True if the RP identifier is valid, false otherwise.
     */
    private fun isValidRpId(
        rpId: String,
        signingInfo: SigningInfo,
        callingPackage: String,
    ): Boolean {
        val websiteUrl = "https://$rpId"
        val assetLinkVerifier = AssetLinkVerifier(websiteUrl)
        try {
            // log the info returned.
            return assetLinkVerifier.verify(callingPackage, signingInfo)
        } catch (e: Exception) {
            // Log exception
        }
        return false
    }

    /**
     * This method helps check if the app called is allowlisted through Google Password Manager
     * @param callingAppInfo : Information pertaining to the calling application.
     */
    private fun validatePrivilegedCallingApp(callingAppInfo: CallingAppInfo): String? {
        val privilegedAppsAllowlist = getGPMPrivilegedAppAllowlist()
        if (privilegedAppsAllowlist != null) {
            return try {
                callingAppInfo.getOrigin(
                    privilegedAppsAllowlist,
                )
            } catch (e: IllegalStateException) {
                val message = getString(R.string.incoming_call_is_not_privileged_to_get_the_origin)
                setUpFailureResponseAndFinish(message)
                null
            } catch (e: IllegalArgumentException) {
                val message = getString(R.string.privileged_allowlist_is_not_formatted_properly)
                setUpFailureResponseAndFinish(message)
                null
            }
        }
        val message = "Could not retrieve GPM allowlist"
        setUpFailureResponseAndFinish(message)
        return null
    }

    /**
     * Retrieves the list of privileged apps allowlisted by Google Password Manager (GPM).
     *
     * This method fetches the allowlist from a remote URL ({@link #GPM_ALLOWLIST_URL})
     * in a background thread. The allowlist is expected to be a string.
     *
     * @return The allowlist of privileged apps as a string, or {@code null} if the
     *         allowlist could not be retrieved or if an error occurred during the
     *         retrieval process.
     */
    private fun getGPMPrivilegedAppAllowlist(): String? {
        val gpmAllowlist: String? = runBlocking {
            val allowlist: Deferred<String?> = async(Dispatchers.IO) {
                try {
                    val url = URL(GPM_ALLOWLIST_URL)
                    return@async url.readText()
                } catch (e: Exception) {
                    return@async null
                }
            }
            return@runBlocking allowlist.await()
        }

        return gpmAllowlist
    }

    /**
     * Checks if the client request contains an origin.
     *
     * @param callingAppInfo Information pertaining to the calling application.
     * @return True if the request contains an origin, false otherwise.
     */
    private fun hasRequestContainsOrigin(callingAppInfo: CallingAppInfo): Boolean {
        try {
            callingAppInfo.getOrigin(INVALID_ALLOWLIST)
        } catch (e: IllegalStateException) {
            return true
        }
        return false
    }

    /**
     * Converts a COSE key to an SPKI (Subject Public Key Info) byte array.
     *
     * @param coseKey A mutable map representing the COSE key.
     * @return The SPKI byte array, or null if an error occurs.
     */
    private fun coseKeyToSPKI(coseKey: MutableMap<Int, Any>): ByteArray? {
        try {
            val spkiPrefix: ByteArray = Base64.decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE", 0)
            val x = coseKey[-2] as ByteArray
            val y = coseKey[-3] as ByteArray
            return spkiPrefix + x + y
        } catch (e: Exception) {
            // Log exceptions
        }
        return null
    }

    /**
     * Set intent response to send back to the calling Relying party/client app
     * @param credentialId : generated credential ID
     * @param response : response to be sent back to calling app
     */
    private fun setIntentForCredentialCredentialResponse(
        credentialId: ByteArray,
        response: AuthenticatorAttestationResponse,
    ) {
        val credential = FidoPublicKeyCredential(
            rawId = credentialId,
            response = response,
            authenticatorAttachment = getString(R.string.platform),
        )
        val intent = Intent()
        // Construct a CreatePublicKeyCredentialResponse with the JSON generated above.
        val publicKeyResponse = CreatePublicKeyCredentialResponse(credential.json())

        // Set CreatePublicKeyCredentialResponse as an extra on an Intent through PendingIntentHandler.setCreateCredentialResponse(),
        // and set that intent to the result of the Activity.
        PendingIntentHandler.setCreateCredentialResponse(intent, publicKeyResponse)
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * Surfaces the biometric prompt to use the screen lock.
     *
     * @param biometricPrompt The biometric prompt to use.
     */
    private fun authenticate(
        biometricPrompt: BiometricPrompt,
    ) {
        val promptInfo = Builder()
            .setTitle(getString(R.string.use_your_screen_lock))
            .setSubtitle(getString(R.string.use_fingerprint))
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Converts an ECPublicKey to a COSE key.
     *
     * @param key The ECPublicKey to convert.
     * @return A mutable map representing the COSE key.
     */
    private fun publicKeyToCose(key: ECPublicKey): MutableMap<Int, Any> {
        val x = bigIntToFixedArray(key.w.affineX)
        val y = bigIntToFixedArray(key.w.affineY)
        val coseKey = mutableMapOf<Int, Any>()
        coseKey[1] = 2 // EC Key type
        coseKey[3] = -7 // ES256
        coseKey[-1] = 1 // P-265 Curve
        coseKey[-2] = x // x
        coseKey[-3] = y // y
        return coseKey
    }

    /**
     * Converts a {@link BigInteger} to a fixed-size byte array of length 32.
     *
     * This method takes a non-negative {@link BigInteger} and converts it into a
     * fixed-size byte array of length 32. If the {@link BigInteger} requires fewer
     * than 32 bytes to represent, it will be right-padded with zeros. If the
     * {@link BigInteger} is larger than 32 bytes, an assertion error will be thrown.
     *
     * The {@link BigInteger#toByteArray()} method may add a leading zero byte if the
     * most-significant bit of the first byte is one. This method handles this case by
     * removing the leading zero byte before padding.
     *
     * @param n The non-negative {@link BigInteger} to convert.
     * @return A byte array of length 32 representing the {@link BigInteger}.
     * @throws AssertionError If the input {@link BigInteger} is negative or requires more than 32 bytes.
     */
    private fun bigIntToFixedArray(n: BigInteger): ByteArray {
        assert(n.signum() >= 0)

        val bytes = n.toByteArray()
        // `toByteArray` will left-pad with a leading zero if the
        // most-significant bit of the first byte would otherwise be one.
        var offset = 0
        if (bytes[0] == 0x00.toByte()) {
            offset++
        }
        val bytesLen = bytes.size - offset
        assert(bytesLen <= 32)

        val output = ByteArray(32)
        System.arraycopy(bytes, offset, output, 32 - bytesLen, bytesLen)
        return output
    }

    /**
     * Updates the metadata in shared preferences.
     *
     * @param accountId The account ID.
     */
    private fun updateMetaInSharedPreferences(accountId: String?) {
        if (accountId == null || (accountId != USER_ACCOUNT)) {
            // AccountId was not set
        } else {
            applicationContext.getSharedPreferences(
                applicationContext.packageName,
                MODE_PRIVATE,
            ).edit().apply {
                putLong(
                    KEY_ACCOUNT_LAST_USED_MS,
                    Instant.now().toEpochMilli(),
                )
            }.apply()
        }
    }

    /**
     * Saves the passkey in the credentials data store.
     *
     * @param request The public key credential creation options.
     * @param credId The credential ID.
     * @param keyPair The key pair.
     */
    private fun savePasskeyInCredentialsDataStore(
        request: PublicKeyCredentialCreationOptions,
        credId: ByteArray,
        keyPair: KeyPair,
    ) {
        runBlocking {
            credentialsDataSource.addNewPasskey(
                PasskeyMetadata(
                    uid = b64Encode(request.user.id),
                    rpid = request.rp.id,
                    username = request.user.name,
                    displayName = request.user.displayName,
                    credId = b64Encode(credId),
                    credPrivateKey = b64Encode((keyPair.private as ECPrivateKey).s.toByteArray()),
                    lastUsedTimeMs = Instant.now().toEpochMilli(),
                ),
            )
        }
    }

    companion object {
        private const val INVALID_ALLOWLIST = "{\"apps\": [\n" +
            "   {\n" +
            "      \"type\": \"android\", \n" +
            "      \"info\": {\n" +
            "         \"package_name\": \"androidx.credentials.test\",\n" +
            "         \"signatures\" : [\n" +
            "         {\"build\": \"release\",\n" +
            "             \"cert_fingerprint_sha256\": \"HELLO\"\n" +
            "         },\n" +
            "         {\"build\": \"ud\",\n" +
            "         \"cert_fingerprint_sha256\": \"YELLOW\"\n" +
            "         }]\n" +
            "      }\n" +
            "    }\n" +
            "]}\n" +
            "\n"

        private const val GPM_ALLOWLIST_URL =
            "https://www.gstatic.com/gpm-passkeys-privileged-apps/apps.json"

        private const val TAG = "MyVault"
        const val KEY_ACCOUNT_LAST_USED_MS = "key_account_last_used_ms"
        const val KEY_ACCOUNT_ID = "key_account_id"
        const val USER_ACCOUNT = "user_account"
    }
}
