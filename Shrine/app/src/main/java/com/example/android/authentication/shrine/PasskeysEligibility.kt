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
package com.example.android.authentication.shrine

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * A class that provides information about whether passkeys are supported on the device.
 */
class PasskeysEligibility {

    companion object {

        private const val MIN_PLAY_VERSION = 230815045

        /**
         * Check if passkeys are supported on the device. In order, we verify that:
         * 1. The API Version >= P
         * 2. Ensure GMS is enabled, to avoid any disabled related errors.
         * 3. Google Play Services >= 230815045, which is a version matching one of the first
         * stable passkey releases. This check is added to the library here:
         * https://developer.android.com/jetpack/androidx/releases/credentials#1.3.0-alpha01
         * 4. The device is secured with some lock.
         *
         * @param context The application context.
         * @return A PasskeysEligibilityData object containing the eligibility status and reason.
         * */
        fun isPasskeySupported(context: Context): PasskeysEligibilityData {
            // Check if device is running on Android P or higher
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return PasskeysEligibilityData(
                    false,
                    context.getString(R.string.lower_than_android_o),
                )
            }

            // Check if Google Play Services disabled
            if (isGooglePlayServicesDisabled(context)) {
                return PasskeysEligibilityData(
                    false,
                    context.getString(R.string.play_services_disabled),
                )
            }

            // Check if Google Play Services version meets minimum requirement
            val playServicesVersion = determineGooglePLayServicesVersion(context)
            if (playServicesVersion < MIN_PLAY_VERSION) {
                return PasskeysEligibilityData(
                    false,
                    context.getString(R.string.google_play_version_low),
                )
            }

            // Check if device is secured with a lock screen
            val isDeviceSecured =
                (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

            if (!isDeviceSecured) {
                return PasskeysEligibilityData(
                    false,
                    context.getString(R.string.device_not_secure),
                )
            }

            // All checks passed, device should support passkeys
            return PasskeysEligibilityData(true, context.getString(R.string.empty_string))
        }

        /**
         * Recovers the current GMS version code running on the device. This is needed because
         * even if a dependency knows the methods and functions of a newer code, the device may
         * only contain the older module, which can cause exceptions due to the discrepancy.
         *
         * @param context The application context.
         * @return The GMS version code.
         */
        @RequiresApi(Build.VERSION_CODES.P)
        private fun determineGooglePLayServicesVersion(context: Context): Long {
            val packageManager: PackageManager = context.packageManager
            val packageName = GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE
            return packageManager.getPackageInfo(packageName, 0).longVersionCode
        }

        /**
         * Determines if Google Play Services is disabled on the device.
         *
         * @param context The application context.
         * @return True if Google Play Services is disabled, false otherwise.
         */
        private fun isGooglePlayServicesDisabled(context: Context): Boolean {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(context)
            return connectionResult != ConnectionResult.SUCCESS
        }
    }
}

/**
 * A data class that contains the eligibility status and reason.
 */
data class PasskeysEligibilityData(
    val isEligible: Boolean,
    val reason: String,
)
