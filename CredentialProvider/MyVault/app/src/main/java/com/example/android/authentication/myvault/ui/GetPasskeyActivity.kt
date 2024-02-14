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
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.PromptInfo.Builder
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.data.PasskeyItem
import com.example.android.authentication.myvault.fido.AssetLinkVerifier
import com.example.android.authentication.myvault.fido.AuthenticatorAssertionResponse
import com.example.android.authentication.myvault.fido.FidoPublicKeyCredential
import com.example.android.authentication.myvault.fido.PublicKeyCredentialRequestOptions
import com.example.android.authentication.myvault.fido.appInfoToOrigin
import com.example.android.authentication.myvault.fido.b64Decode
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.net.URL
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import java.time.Instant

/*
* This class is responsible for handling the public key credential (Passkey) get request from a Relying Party i.e calling app
 */
class GetPasskeyActivity : FragmentActivity() {

    private val credentialsDataSource = AppDependencies.credentialsDataSource

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleGetPasskeyIntent()
    }

    /**
     *  Handle the intent for get public key credential (passkey)
     */
    private fun handleGetPasskeyIntent() {
        val requestInfo = intent.getBundleExtra(getString(R.string.vault_data))
        intent.getBooleanExtra(getString(R.string.is_auto_selected), false)

        /*
         * retrieveProviderGetCredentialRequest extracts the [ProviderGetCredentialRequest] from the provider's
         * [PendingIntent] invoked by the Android system, when the user selects a
         * [CredentialEntry].
         */
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        if (requestInfo == null || request == null) {
            setUpFailureResponseAndFinish(getString(R.string.unable_to_retrieve_data_from_intent))
            return
        }

        val req = request.credentialOptions[0]

        // Extract the GetPublicKeyCredentialOption from the request retrieved above.
        val publicKeyRequest = req as GetPublicKeyCredentialOption
        val publicKeyRequestOptions = PublicKeyCredentialRequestOptions(
            publicKeyRequest.requestJson,
        )
        var callingAppOriginInfo: String? = null

        if (hasRequestContainsOrigin(request.callingAppInfo)) {
            callingAppOriginInfo = validatePrivilegedCallingApp(
                request.callingAppInfo,
            )
        } else {
            // Native call. Check for asset links to verify app's identity
            validateAssetLinks(
                publicKeyRequestOptions.rpId,
                request.callingAppInfo,
            )
        }

        // Extract the requestJson and clientDataHash from this option.
        var clientDataHash: ByteArray? = null
        if (request.callingAppInfo.origin != null) {
            clientDataHash = publicKeyRequest.clientDataHash
        }

        if (callingAppOriginInfo != null) {
            clientDataHash = publicKeyRequest.clientDataHash
        }

        assertPasskey(
            request.callingAppInfo,
            clientDataHash,
            requestInfo,
            publicKeyRequest.requestJson,
            callingAppOriginInfo,
        )
    }

    /**
     * This method helps check the asset linking to verify client app idenity
     * @param rpId : Relying party identifier
     * @param callingAppInfo : Information pertaining to the calling application.
     */
    private fun validateAssetLinks(rpId: String, callingAppInfo: CallingAppInfo) {
        val isRpValid: Boolean = runBlocking {
            val isRpValidDeferred: Deferred<Boolean> = async(Dispatchers.IO) {
                return@async isValidRpId(
                    rpId,
                    callingAppInfo.signingInfo,
                    callingAppInfo.packageName,
                )
            }
            return@runBlocking isRpValidDeferred.await()
        }

        if (!isRpValid) {
            setUpFailureResponseAndFinish("Failed to validate rp")
            return
        }
    }

    /**
     * Validates the Relying Party (RP) identifier using asset linking.
     *
     * @param rpId The identifier of the RP.
     * @param signingInfo The signing information of the calling app.
     * @param callingPackage The package name of the calling app.
     * @return True if the RP is valid, false otherwise.
     */
    private fun isValidRpId(
        rpId: String,
        signingInfo: SigningInfo,
        callingPackage: String,
    ): Boolean {
        val websiteUrl = "https://$rpId"
        val assetLinkVerifier = AssetLinkVerifier(websiteUrl)
        try {
            return assetLinkVerifier.verify(callingPackage, signingInfo)
        } catch (e: Exception) {
            // Log exception
        }
        return false
    }

    /**
     * Validates if the app is privileged to get the origin, i.e., allowlisted in GPM privileged apps.
     *
     * @param callingAppInfo Information pertaining to the calling application.
     * @return The origin if the app is privileged, or null otherwise.
     */
    private fun validatePrivilegedCallingApp(callingAppInfo: CallingAppInfo): String? {
        val privAppAllowlistJson = getGPMPrivilegedAppAllowlist()
        if (privAppAllowlistJson != null) {
            return try {
                callingAppInfo.getOrigin(
                    privAppAllowlistJson,
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
     *
     * @return The allowlist as a JSON string, or null if there is an error.
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
     * Checks if the client request contains an origin for the calling app.
     *
     * @param callingAppInfo Information pertaining to the calling application.
     * @return True if the request contains an origin, false otherwise.
     */
    private fun hasRequestContainsOrigin(callingAppInfo: CallingAppInfo): Boolean {
        try {
            callingAppInfo.getOrigin(INVALID_ALLOWLIST)
        } catch (e: IllegalStateException) {
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
        return false
    }

    /**
     * Sets up a failure response and finishes the activity.
     *
     * @param message The error message to include in the response.
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
     * Confirm that the passkey is valid with extracted metadata, and user verification.
     *
     * @param callingAppInfo Information pertaining to the calling application.
     * @param clientDataHash a clientDataHash value to sign over in place of assembling and hashing
     * @param requestInfo  type of information requested
     * @param requestJson  calling app metadata
     * @param callingAppOriginInfo information if app is priviliged to get the origin
     *
     */
    private fun assertPasskey(
        callingAppInfo: CallingAppInfo,
        clientDataHash: ByteArray?,
        requestInfo: Bundle,
        requestJson: String,
        callingAppOriginInfo: String?,
    ) {
        val credIdEnc = requestInfo.getString(getString(R.string.cred_id))!!
        val passkey = credentialsDataSource.getPasskey(credIdEnc)!!

        val credId = b64Decode(credIdEnc)
        val privateKey = b64Decode(passkey.credPrivateKey)
        val uid = b64Decode(passkey.uid)

        val origin = appInfoToOrigin(callingAppInfo)
        val packageName = callingAppInfo.packageName

        val request = PublicKeyCredentialRequestOptions(requestJson)
        val convertedPrivateKey = convertPrivateKey(privateKey)

        val biometricPrompt = configureBioMetricPrompt(
            passkey,
            origin,
            callingAppOriginInfo,
            request,
            uid,
            packageName,
            clientDataHash,
            convertedPrivateKey,
            credId,
        )

        authenticate(biometricPrompt)
    }

    /**
     * Configures the BiometricPrompt with authentication callbacks.
     *
     * @param passkey The PasskeyItem associated with the authentication.
     * @param origin The origin of the calling app.
     * @param callingAppInfoOrigin The origin of the calling app if it is privileged.
     * @param request The PublicKeyCredentialRequestOptions for the authentication.
     * @param uid The user ID for the authentication.
     * @param packageName The package name of the calling app.
     * @param clientDataHash The client data hash for the authentication.
     * @param convertedPrivateKey The converted private key for the authentication.
     * @param credId The credential ID for the authentication.
     *
     * @return The configured BiometricPrompt.
     */
    private fun configureBioMetricPrompt(
        passkey: PasskeyItem,
        origin: String,
        callingAppInfoOrigin: String?,
        request: PublicKeyCredentialRequestOptions,
        uid: ByteArray,
        packageName: String,
        clientDataHash: ByteArray?,
        convertedPrivateKey: ECPrivateKey,
        credId: ByteArray,
    ): BiometricPrompt {
        val biometricPrompt = BiometricPrompt(
            this,
            mainExecutor,
            object : AuthenticationCallback() {
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
                    result: AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)

                    updatePasskeyInCredentialsDataSource(passkey)

                    var callingOrigin = origin
                    if (callingAppInfoOrigin != null) {
                        callingOrigin = callingAppInfoOrigin
                    }

                    configureGetCredentialResponse(
                        request,
                        origin = callingOrigin,
                        uid,
                        packageName,
                        clientDataHash,
                        convertedPrivateKey,
                        credId,
                    )
                }
            },
        )
        return biometricPrompt
    }

    /**
     * To validate the user, surface a Biometric prompt (or other assertion method).
     *
     * @param biometricPrompt The BiometricPrompt object to use for authentication.
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
     * Once the authentication succeeds, construct a JSON response based on the W3 Web Authentication Assertion spec.
     *
     * Construct a PublicKeyCredential using the JSON generated above and set it on a final GetCredentialResponse.
     *
     * Set this final response on the result of this activity.
     *
     * @param request The PublicKeyCredentialRequestOptions object.
     * @param origin The origin of the calling app.
     * @param uid The user ID.
     * @param packageName The package name of the calling app.
     * @param clientDataHash The client data hash.
     * @param privateKey The private key.
     * @param credId The credential ID.
     */
    private fun configureGetCredentialResponse(
        request: PublicKeyCredentialRequestOptions,
        origin: String,
        uid: ByteArray,
        packageName: String,
        clientDataHash: ByteArray?,
        privateKey: ECPrivateKey,
        credId: ByteArray,
    ) {
        val response = AuthenticatorAssertionResponse(
            requestOptions = request,
            origin = origin,
            up = true,
            uv = true,
            be = true,
            bs = true,
            userHandle = uid,
            packageName = packageName,
            clientDataHash,
        )

        val signature = Signature.getInstance(getString(R.string.sha256_with_ecdsa))
        signature.initSign(privateKey)
        signature.update(response.dataToSign())
        response.signature = signature.sign()

        val credential = FidoPublicKeyCredential(
            rawId = credId,
            response = response,
            authenticatorAttachment = getString(R.string.platform),
        )
        val intent = Intent()
        val cred = PublicKeyCredential(credential.json())
        PendingIntentHandler.setGetCredentialResponse(
            intent,
            GetCredentialResponse(cred),
        )
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun updatePasskeyInCredentialsDataSource(passkeyItem: PasskeyItem) {
        runBlocking {
            credentialsDataSource.updatePasskey(
                passkeyItem.copy(
                    lastUsedTimeMs = Instant.now().toEpochMilli(),
                ),
            )
        }
    }

    /**
     * Encrypts the private key. This is used for demonstration purposes.
     *
     * @param privateKeyBytes The private key bytes to encrypt.
     * @return The encrypted private key.
     */
    private fun convertPrivateKey(privateKeyBytes: ByteArray): ECPrivateKey {
        val params = AlgorithmParameters.getInstance(getString(R.string.ec))
        params.init(ECGenParameterSpec(getString(R.string.secp_256_r1)))
        val spec = params.getParameterSpec(ECParameterSpec::class.java)

        // Convert the private key bytes to a BigInteger.
        val bi = BigInteger(1, privateKeyBytes)
        // Create an EC private key specification from the BigInteger and the EC parameter specification.
        val privateKeySpec = ECPrivateKeySpec(bi, spec)

        val keyFactory = KeyFactory.getInstance(getString(R.string.ec))

        // Generate the encrypted private key using the KeyFactory.
        return keyFactory.generatePrivate(privateKeySpec) as ECPrivateKey
    }

    companion object {
        // This is to check if the origin was populated.
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
    }
}
