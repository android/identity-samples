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

        // access the associated intent and pass it into the PendingIntentHandler class to get the ProviderCreateCredentialRequest.
        if (request.callingRequest is CreatePublicKeyCredentialRequest) {
            val publicKeyRequest: CreatePublicKeyCredentialRequest =
                request.callingRequest as CreatePublicKeyCredentialRequest
            createPasskey(
                publicKeyRequest.requestJson,
                request.callingAppInfo,
                publicKeyRequest.clientDataHash,
                accountId,
            )
        } else {
            setUpFailureResponseAndFinish("Unexpected create request found in intent")
            return
        }
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
    private fun createPasskey(
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
            },
        )
        authenticate(biometricPrompt)
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
            setUpFailureResponseAndFinish("Failed to validate rp")
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
                val message = "Incoming call is not privileged to get the origin"
                setUpFailureResponseAndFinish(message)
                null
            } catch (e: IllegalArgumentException) {
                val message = "Privileged allowlist is not formatted properly"
                setUpFailureResponseAndFinish(message)
                null
            }
        }
        val message = "Could not retrieve GPM allowlist"
        setUpFailureResponseAndFinish(message)
        return null
    }

    /**
     * Method to retrieve the list of privileged apps allowlisted by GPM
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
}
