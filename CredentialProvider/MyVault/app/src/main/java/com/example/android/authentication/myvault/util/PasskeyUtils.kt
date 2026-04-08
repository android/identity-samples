package com.example.android.authentication.myvault.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.SigningInfo
import android.util.Base64
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.provider.CallingAppInfo
import com.example.android.authentication.myvault.data.CredentialsDataSource
import com.example.android.authentication.myvault.data.PasskeyMetadata
import com.example.android.authentication.myvault.fido.AssetLinkVerifier
import com.example.android.authentication.myvault.fido.AuthenticatorAttestationResponse
import com.example.android.authentication.myvault.fido.Cbor
import com.example.android.authentication.myvault.fido.FidoPublicKeyCredential
import com.example.android.authentication.myvault.fido.PublicKeyCredentialCreationOptions
import com.example.android.authentication.myvault.fido.appInfoToOrigin
import com.example.android.authentication.myvault.fido.b64Encode
import com.example.android.authentication.myvault.ui.CreatePasskeyActivity.Companion.KEY_ACCOUNT_LAST_USED_MS
import com.example.android.authentication.myvault.ui.CreatePasskeyActivity.Companion.TAG
import com.example.android.authentication.myvault.ui.CreatePasskeyActivity.Companion.USER_ACCOUNT
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

class PasskeyUtils {

    companion object {
        /**
         * Wrapper to call isValidRpId with a dispatcher.
         *
         * * @param rpId The RP identifier to validate.
         *   @param signingInfo The signing information of the calling application.
         *   @return True if the RP is valid, false otherwise.
         */
        fun checkRpValidity(rpId: String, callingAppInfo: CallingAppInfo): Boolean {
            val isRpValid = runBlocking {
                var isRpValidDeferred: Deferred<Boolean> = async(Dispatchers.IO) {
                    val isValidRpId = isValidRpId(
                        rpId,
                        callingAppInfo.signingInfo,
                        callingAppInfo.packageName,
                    )
                    return@async isValidRpId
                }
                return@runBlocking isRpValidDeferred.await()
            }

            return isRpValid
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
                Log.e(TAG, "Error verifying asset links", e)
                // Log exception
            }
            return false
        }

        fun validatePrivilegedCallingApp(
            callingAppInfo: CallingAppInfo,
        ): PrivilegedValidationResult {
            val privilegedAppsAllowlist = getGPMPrivilegedAppAllowlist()
                ?: return PrivilegedValidationResult.Failure.AllowlistMissing

            return try {
                val origin = callingAppInfo.getOrigin(privilegedAppsAllowlist)
                PrivilegedValidationResult.Success(origin)
            } catch (e: IllegalStateException) {
                PrivilegedValidationResult.Failure.NotPrivileged
            } catch (e: IllegalArgumentException) {
                PrivilegedValidationResult.Failure.BadFormat
            }
        }

        fun getGPMPrivilegedAppAllowlist(): String? {
            return runBlocking {
                try {
                    // Read the text from the URL safely on the IO thread
                    URL(GPM_ALLOWLIST_URL).readText()
                } catch (e: Exception) {
                    // If the network fails, or the URL is bad, return null
                    null
                }
            }
        }

        fun createAndStorePasskey(
            applicationContext: Context,
            credentialsDataSource: CredentialsDataSource,
            request: PublicKeyCredentialCreationOptions,
            callingAppInfo: CallingAppInfo,
            callingAppInfoOrigin: String?,
            clientDataHash: ByteArray?,
            accountId: String?,
        ): CreatePublicKeyCredentialResponse {
            // Generate CredentialID
            val credentialId = ByteArray(32)
            SecureRandom().nextBytes(credentialId)

            // Generate key
            val keyPair = generateKeyPair()

            // Save the private key in your local database against callingAppInfo.packageName.
            savePasskeyInCredentialsDataStore(credentialsDataSource, request, credentialId, keyPair)

            updateMetaInSharedPreferences(applicationContext, accountId)

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

            val credential = FidoPublicKeyCredential(
                rawId = credentialId,
                response = response,
                authenticatorAttachment = platform,
            )

            // Construct a CreatePublicKeyCredentialResponse with the JSON generated above.
            return CreatePublicKeyCredentialResponse(credential.json())

            // return this back to Activity or Service?
            // in Activity code, sets pendingIntentHandler with the publicKeyResponse
            // in reema's code: callback.onResult(publicKeyResponse)
        }

        /**
         * Generates a new key pair for use in creating a public key credential.
         *
         * @return A new [KeyPair] instance.
         */
        private fun generateKeyPair(): KeyPair {
            val spec = ECGenParameterSpec(secp_256_r1)
            val keyPairGen = KeyPairGenerator.getInstance(ec)
            keyPairGen.initialize(spec)
            return keyPairGen.genKeyPair()
        }

        /**
         * Saves the passkey in the credentials data store.
         *
         * @param request The public key credential creation options.
         * @param credId The credential ID.
         * @param keyPair The key pair.
         */
        private fun savePasskeyInCredentialsDataStore(
            credentialsDataSource: CredentialsDataSource,
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

        /**
         * Updates the metadata in shared preferences.
         *
         * @param accountId The account ID.
         */
        private fun updateMetaInSharedPreferences(applicationContext: Context, accountId: String?) {
            if (accountId == USER_ACCOUNT) {
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

        private const val GPM_ALLOWLIST_URL =
            "https://www.gstatic.com/gpm-passkeys-privileged-apps/apps.json"
        private val secp_256_r1 = "secp256r1"
        private val ec = "EC"
        private val platform = "platform"
    }
}

sealed class PrivilegedValidationResult {
    data class Success(val origin: String?) : PrivilegedValidationResult()

    sealed class Failure : PrivilegedValidationResult() {
        object AllowlistMissing : Failure()
        object NotPrivileged : Failure()
        object BadFormat : Failure()
    }
}
