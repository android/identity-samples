/*
 * Copyright 2025 Google LLC
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

package com.google.credentialmanager.sample

import android.content.Context
import android.content.SharedPreferences

/**
 * A data provider that manages the user's signed-in status using SharedPreferences.
 */
object DataProvider {

    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private const val IS_SIGNED_IN = "isSignedIn"
    private const val IS_SIGNED_IN_THROUGH_PASSKEYS = "isSignedInThroughPasskeys"
    private const val PREF_NAME = "CREDMAN_PREF"

    /**
     * Initializes the SharedPreferences for the data provider.
     *
     * @param context The application context.
     */
    fun initSharedPref(context: Context) {
        sharedPreference =
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = sharedPreference.edit()
    }

    /**
     * Sets the user's signed-in status.
     *
     * @param flag True if the user is signed in, false otherwise.
     */
    fun configureSignedInPref(flag: Boolean) {
        editor.putBoolean(IS_SIGNED_IN, flag)
        editor.commit()
    }

    /**
     * Sets whether the user signed in through passkeys.
     *
     * @param flag True if the user signed in through passkeys, false otherwise.
     */
    fun setSignedInThroughPasskeys(flag: Boolean) {
        editor.putBoolean(IS_SIGNED_IN_THROUGH_PASSKEYS, flag)
        editor.commit()
    }

    /**
     * Checks if the user is signed in.
     *
     * @return True if the user is signed in, false otherwise.
     */
    fun isSignedIn(): Boolean {
        return sharedPreference.getBoolean(IS_SIGNED_IN, false)
    }

    /**
     * Checks if the user signed in through passkeys.
     *
     * @return True if the user signed in through passkeys, false otherwise.
     */
    fun isSignedInThroughPasskeys(): Boolean {
        return sharedPreference.getBoolean(IS_SIGNED_IN_THROUGH_PASSKEYS, false)
    }
}
