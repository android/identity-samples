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
import androidx.credentials.provider.BiometricPromptResult
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

        val biometricPromptResult = request.biometricPromptResult

        if (isValidBiometricFlowError(biometricPromptResult)) return

        val publicKeyCredentialOption = request.credentialOptions[0]

        // Extract the GetPublicKeyCredentialOption from the request retrieved above.
        val publicKeyRequest = publicKeyCredentialOption as GetPublicKeyCredentialOption
        val publicKeyRequestOptions = PublicKeyCredentialRequestOptions(
            publicKeyRequest.requestJson,
        )

        val credentialIdEncoded = requestInfo.getString(getString(R.string.cred_id))!!
        val passkey = credentialsDataSource.getPasskey(credentialIdEncoded)!!

        val credentialID = b64Decode(credentialIdEncoded)
        val privateKey = b64Decode(passkey.credPrivateKey)
        val uid = b64Decode(passkey.uid)
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

        val origin = appInfoToOrigin(request.callingAppInfo)
        val packageName = request.callingAppInfo.packageName

        val convertedPrivateKey = convertPrivateKey(privateKey)

        // Extract the requestJson and clientDataHash from this option.
        var clientDataHash: ByteArray? = null
        if (callingAppOriginInfo != null) {
            clientDataHash = publicKeyRequest.clientDataHash
        }

        configurePasskeyAssertion(
            biometricPromptResult,
            passkey,
            origin,
            callingAppOriginInfo,
            publicKeyRequestOptions,
            uid,
            packageName,
            clientDataHash,
            convertedPrivateKey,
            credentialID,
        )
    }

    /**
     * Configures the passkey assertion based on the biometric authentication result.
     *
     * <p>This method determines whether to use the biometric flow or the default
     * flow for asserting a passkey. If the {@link BiometricPromptResult} indicates
     * successful authentication, it calls {@link #assertPasskeyWithBiometricFlow}
     * to proceed with the biometric flow. Otherwise, it calls
     * {@link #assertPasskeyWithDefaultFlow} to proceed with the default flow.
     *
     * @param biometricPromptResult The result of the biometric authentication prompt.
     * @param passkey               The {@link PasskeyItem} containing the passkey details.
     * @param origin                The origin of the calling application.
     * @param callingAppOriginInfo  The origin information of the calling application, if available.
     * @param publicKeyRequestOptions The {@link PublicKeyCredentialRequestOptions} containing the
     *                                request details.
     * @param uid                   The unique identifier associated with the passkey.
     * @param packageName           The package name of the calling application.
     * @param clientDataHash        The client data hash, if available.
     * @param convertedPrivateKey   The converted private key for the passkey.
     * @param credentialID          The credential ID of the passkey.
     */
    private fun configurePasskeyAssertion(
        biometricPromptResult: BiometricPromptResult?,
        passkey: PasskeyItem,
        origin: String,
        callingAppOriginInfo: String?,
        publicKeyRequestOptions: PublicKeyCredentialRequestOptions,
        uid: ByteArray,
        packageName: String,
        clientDataHash: ByteArray?,
        convertedPrivateKey: ECPrivateKey,
        credentialID: ByteArray,
    ) {
        // Check if the biometric prompt result indicates successful authentication.
        if (biometricPromptResult?.authenticationResult != null) {
            // If biometric authentication was successful, use the biometric flow.
            assertPasskeyWithBiometricFlow(
                passkey,
                origin,
                callingAppOriginInfo,
                publicKeyRequestOptions,
                uid,
                packageName,
                clientDataHash,
                convertedPrivateKey,
                credentialID,
            )
        } else {
            // If biometric authentication was not used or was not successful, use the default flow.
            assertPasskeyWithDefaultFlow(
                passkey,
                origin,
                callingAppOriginInfo,
                publicKeyRequestOptions,
                uid,
                clientDataHash,
                convertedPrivateKey,
                credentialID,
            )
        }
    }

    /**
     * Checks if there was an error during the biometric authentication flow.
     *
     * <p>This method determines whether the biometric authentication flow resulted in
     * an error. It checks if the {@link BiometricPromptResult} is null or if the
     * authentication was successful. If neither of these conditions is met, it
     * extracts the error code and message from the {@link BiometricPromptResult},
     * constructs an error message, and sets up a failure response to be sent to
     * the client.
     *
     * <p>The error message is built using the following format:
     * "Biometric Error Code: [errorCode] [errorMessage] Other providers may be available."
     *
     * @param biometricPromptResult The result of the biometric authentication prompt.
     * @return True if there was an error during the biometric flow, false otherwise.
     */
    @SuppressLint("StringFormatMatches")
    private fun isValidBiometricFlowError(biometricPromptResult: BiometricPromptResult?): Boolean {
        // If the biometricPromptResult is null, there was no error.
        if (biometricPromptResult == null) return false

        // If the biometricPromptResult indicates success, there was no error.
        if (biometricPromptResult.isSuccessful) return false

        // Initialize default values for the error code and message.
        var biometricAuthErrorCode = -1
        var biometricAuthErrorMsg = getString(R.string.unknown_failure)

        // Check if there is an authentication error in the biometricPromptResult.
        if (biometricPromptResult.authenticationError != null) {
            // Extract the error code and message from the authentication error.
            biometricAuthErrorCode = biometricPromptResult.authenticationError!!.errorCode
            biometricAuthErrorMsg = biometricPromptResult.authenticationError!!.errorMsg.toString()
        }

        // Build the error message to be sent to the client.
        val errorMessage = buildString {
            append(
                getString(
                    R.string.biometric_error_code_with_message,
                    biometricAuthErrorCode,
                ),
            )
            append(biometricAuthErrorMsg)
            append(getString(R.string.other_providers_error_message))
        }

        // Set up the failure response and finish the flow with the constructed error message.
        setUpFailureResponseAndFinish(errorMessage)

        // Indicate that there was an error during the biometric flow.
        return true
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
        val allowlistJson = getGPMPrivilegedAppAllowlist()
        if (allowlistJson != null) {
            return try {
                callingAppInfo.getOrigin(
                    allowlistJson,
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
        val message = getString(R.string.could_not_retrieve_gpm_allowlist)
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
     * Asserts the passkey using the default flow, which involves presenting a biometric prompt.
     *
     * <p>This method is called when the biometric authentication flow is not used or
     * was not successful. It configures a {@link BiometricPrompt} using the provided
     * parameters and then initiates the authentication process. The authentication
     * process will prompt the user to authenticate using biometrics or device
     * credentials.
     *
     * @param passkey               The {@link PasskeyItem} containing the passkey details.
     * @param origin                The origin of the calling application.
     * @param callingAppInfo        The origin information of the calling application, if available.
     * @param publicKeyRequestOptions The {@link PublicKeyCredentialRequestOptions} containing the
     *                                request details.
     * @param uid                   The unique identifier associated with the passkey.
     * @param clientDataHash        The client data hash, if available.
     * @param convertedPrivateKey   The converted private key for the passkey.
     * @param credId                The credential ID of the passkey.
     */
    private fun assertPasskeyWithDefaultFlow(
        passkey: PasskeyItem,
        origin: String,
        callingAppInfo: String?,
        publicKeyRequestOptions: PublicKeyCredentialRequestOptions,
        uid: ByteArray,
        clientDataHash: ByteArray?,
        convertedPrivateKey: ECPrivateKey,
        credId: ByteArray,
    ) {
        // Configure the BiometricPrompt with the provided parameters.
        val biometricPrompt = configureBioMetricPrompt(
            passkey,
            origin,
            callingAppInfo,
            publicKeyRequestOptions,
            uid,
            packageName,
            clientDataHash,
            convertedPrivateKey,
            credId,
        )
        // Initiate the authentication process using the configured BiometricPrompt.
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

                    assertPasskeyWithBiometricFlow(
                        passkey,
                        origin,
                        callingAppInfoOrigin,
                        request,
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
     * Asserts the passkey using the biometric flow.
     *
     * <p>This method is called when the biometric authentication flow is successful.
     * It updates the passkey's last used time in the data source and then
     * configures the credential response to be sent back to the calling application.
     *
     * @param passkey               The {@link PasskeyItem} containing the passkey details.
     * @param origin                The origin of the calling application.
     * @param callingAppInfoOrigin  The origin information of the calling application, if available.
     * @param request               The {@link PublicKeyCredentialRequestOptions} containing the
     *                              request details.
     * @param uid                   The unique identifier associated with the passkey.
     * @param packageName           The package name of the calling application.
     * @param clientDataHash        The client data hash, if available.
     * @param convertedPrivateKey   The converted private key for the passkey.
     * @param credId                The credential ID of the passkey.
     */
    private fun assertPasskeyWithBiometricFlow(
        passkey: PasskeyItem,
        origin: String,
        callingAppInfoOrigin: String?,
        request: PublicKeyCredentialRequestOptions,
        uid: ByteArray,
        packageName: String,
        clientDataHash: ByteArray?,
        convertedPrivateKey: ECPrivateKey,
        credId: ByteArray,
    ) {
        // Update the passkey's last used time in the data source.
        updatePasskeyInCredentialsDataSource(passkey)

        // Determine the calling origin. If callingAppInfoOrigin is available, use it; otherwise, use the provided origin.
        var callingOrigin = origin
        if (callingAppInfoOrigin != null) {
            callingOrigin = callingAppInfoOrigin
        }

        // Configure the credential response with the determined origin and other parameters.
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
