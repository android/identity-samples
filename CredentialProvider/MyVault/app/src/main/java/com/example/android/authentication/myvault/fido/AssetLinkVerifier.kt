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
package com.example.android.authentication.myvault.fido

import android.content.pm.SigningInfo
import android.util.Log
import org.json.JSONObject
import java.net.URL
import java.security.MessageDigest

/**
 * Verifies the identity of the client app through asset linking.
 *
 * @param websiteUrl The domain name against which the package name and SHA needs to be verified.
 */
class AssetLinkVerifier(private val websiteUrl: String) {

    /**
     * Verifies the package name and signing info for an app associated with a domain.
     *
     * @param callingPackage The calling package name of the calling app.
     * @param callerSigningInfo The signingInfo associated with the calling app.
     * @return True if the package name and signing info are valid, false otherwise.
     */
    fun verify(callingPackage: String, callerSigningInfo: SigningInfo): Boolean {
        val assetLinkCheckJsonResponse = callDigitalAssetLinkApi(
            websiteUrl,
            callingPackage,
            computeLatestCertification(callerSigningInfo)!!,
        )
        Log.i("AssetLinkVerifier", "Response: $assetLinkCheckJsonResponse")
        return JSONObject(assetLinkCheckJsonResponse).getBoolean("linked")
    }

    /**
     * Computes the latest certification based on the signing info provided for a client app.
     *
     * @param callerSigningInfo The signingInfo associated with the calling app.
     * @return The latest certification, or null if the app has multiple signers.
     */
    private fun computeLatestCertification(callerSigningInfo: SigningInfo): String? {
        if (callerSigningInfo.hasMultipleSigners()) {
            return null
        }
        return computeNormalizedSha256Fingerprint(
            callerSigningInfo.signingCertificateHistory[0].toByteArray(),
        )
    }

    /**
     * Calls the Digital Asset Link API to verify the package name and signing info.
     *
     * @param websiteUrl The associated domain.
     * @param callingPackage The calling package name of the calling app.
     * @param callingCert The latest certification computed for the calling app packagename.
     * @return The response from the Digital Asset Link API.
     */
    private fun callDigitalAssetLinkApi(
        websiteUrl: String,
        callingPackage: String,
        callingCert: String,
    ): String {
        val apiEndpoint = "https://digitalassetlinks.googleapis.com/v1/assetlinks:check" +
            "?source.web.site=$websiteUrl+" +
            "&target.android_app.package_name=$callingPackage" +
            "&target.android_app.certificate.sha256_fingerprint=$callingCert" +
            "&relation=delegate_permission/common.handle_all_urls"
        return URL(apiEndpoint).readText()
    }

    /**
     * Computes the normalized SHA-256 fingerprint of the given signature.
     *
     * @param signature The signature to compute the fingerprint for.
     * @return The normalized SHA-256 fingerprint.
     */
    private fun computeNormalizedSha256Fingerprint(signature: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHexString(digest.digest(signature))
    }

    /**
     * Converts the given bytes to a hexadecimal string.
     *
     * @param bytes The bytes to convert.
     * @return The hexadecimal string representation of the bytes.
     */
    private fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}
