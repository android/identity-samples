/*
 * Copyright 2023 Google LLC
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

object DataProvider {

    private lateinit var sharedPreference: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private const val IS_SIGNED_IN = "isSignedIn"
    private const val IS_SIGNED_IN_THROUGH_PASSKEYS = "isSignedInThroughPasskeys"
    private const val PREF_NAME = "CREDMAN_PREF"

    fun initSharedPref(context: Context) {
        sharedPreference =
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = sharedPreference.edit()
    }

    //Set if the user is signed in or not
    fun configureSignedInPref(flag: Boolean) {
        editor.putBoolean(IS_SIGNED_IN, flag)
        editor.commit()
    }

    //Set if signed in through passkeys or not
    fun setSignedInThroughPasskeys(flag: Boolean) {
        editor.putBoolean(IS_SIGNED_IN_THROUGH_PASSKEYS, flag)
        editor.commit()
    }

    fun isSignedIn(): Boolean {
        return sharedPreference.getBoolean(IS_SIGNED_IN, false)
    }

    fun isSignedInThroughPasskeys(): Boolean {
        return sharedPreference.getBoolean(IS_SIGNED_IN_THROUGH_PASSKEYS, false)
    }
}
