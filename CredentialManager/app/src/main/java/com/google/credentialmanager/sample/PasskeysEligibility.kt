package com.google.credentialmanager.sample

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class PasskeysEligibility {

    companion object {

        private const val MIN_PLAY_VERSION = 230815045
        /**
         * Check if passkeys are supported on the device. In order, we verify that:
         * 1. The API Version >= P
         * 2. Ensure GMS is enabled, to avoid any disabled related errors.
         * 3. Google Play Services >= 230815045, which is a version matching one of the first stable passkey releases.
         * This check is added to the library here: https://developer.android.com/jetpack/androidx/releases/credentials#1.3.0-alpha01
         * 4. The device is secured with some lock.
         */
        fun isPasskeySupported(context: Context): Boolean {

            // Check if device is running on Android P or higher
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return false
            }

            // Check if Google Play Services disabled
            if (isGooglePlayServicesDisabled(context)) {
                return false
            }

            // Check if Google Play Services version meets minimum requirement
            val yourPlayVersion = determineDeviceGMSVersionCode(context)
            if (yourPlayVersion < MIN_PLAY_VERSION) {
                return false
            }

            // Check if device is secured with a lock screen
            val isDeviceSecured =
                (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

            if(!isDeviceSecured) {
                return false
            }

            // All checks passed, device should support passkeys
            return true
        }

        /**
         * Recovers the current GMS version code running on the device. This is needed because
         * even if a dependency knows the methods and functions of a newer code, the device may
         * only contain the older module, which can cause exceptions due to the discrepancy.
         */
        private fun determineDeviceGMSVersionCode(context: Context): Long {
            val packageManager: PackageManager = context.packageManager
            val packageName = GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE
            return packageManager.getPackageInfo(packageName, 0).longVersionCode
        }

        /**
         * Determines if Google Play Services is disabled on the device.
         */
        private fun isGooglePlayServicesDisabled(context: Context): Boolean {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(context)
            return connectionResult != ConnectionResult.SUCCESS
        }
    }
}