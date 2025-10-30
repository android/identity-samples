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

/**
 * A class that provides JSON data from the app's assets.
 *
 * @param context The application context.
 */
class JsonProvider(private val context: Context) {
    /**
     * Fetches the registration JSON from the "RegFromServer" asset.
     *
     * @return The registration JSON as a string.
     */
    fun fetchRegistrationJson(): String {
        return context.assets.open("RegFromServer").bufferedReader().use { it.readText() }
    }

    /**
     * Fetches the authentication JSON from the "AuthFromServer" asset.
     *
     * @return The authentication JSON as a string.
     */
    fun fetchAuthJson(): String {
        return context.assets.open("AuthFromServer").bufferedReader().use { it.readText() }
    }
}
