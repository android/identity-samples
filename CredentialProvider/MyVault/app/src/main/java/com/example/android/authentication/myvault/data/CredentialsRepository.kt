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
package com.example.android.authentication.myvault.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse.Builder
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.fido.PublicKeyCredentialRequestOptions
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class is responsible for creating and retrieving  credential entries (password & passkey) to & from the database
 */
class CredentialsRepository(
    private val sharedPreferences: SharedPreferences,
    private val credentialsDataSource: CredentialsDataSource,
    private val applicationContext: Context,
) {
    private val requestCode: AtomicInteger = AtomicInteger()

    /**
     * This method queries credentials from your database, create passkey and password entries to populate.
     *
     * @param request   The BeginGetPublicKeyCredentialOption object containing the request parameters.
     * @param responseBuilder The Builder object used to build the BeginGetCredentialResponse.
     * @return True if credentials were found and added to the response builder, false otherwise.
     */
    fun processGetCredentialsRequest(
        request: BeginGetCredentialRequest,
        responseBuilder: Builder,
    ): Boolean {
        val callingPackage = request.callingAppInfo?.packageName ?: return false

        var hasFoundCredentials = false

        for (option in request.beginGetCredentialOptions) {
            when (option) {
                // If the chosen option is a Password credential
                is BeginGetPasswordOption -> {
                    if (populatePasswordData(callingPackage, option, responseBuilder)) {
                        hasFoundCredentials = true
                    }
                }

                // If the chosen option is a Passkey credential
                is BeginGetPublicKeyCredentialOption -> {
                    if (populatePasskeyData(option, responseBuilder)) {
                        hasFoundCredentials = true
                    }
                }
            }
        }
        return hasFoundCredentials
    }

    /**
     * This method queries credentials from the storage used i.e database here, create passkey entries to populate.
     *
     * @param request The BeginCreateCredentialRequest object containing the request parameters.
     * @return The BeginCreateCredentialResponse object containing the list of credential entries.
     */
    fun processCreateCredentialsRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        var passwordCount = 0
        var passkeyCount = 0

        val requestJson =
            request.candidateQueryData.getString("androidx.credentials.BUNDLE_KEY_REQUEST_JSON")

        val callingPackage = request.callingAppInfo?.packageName
        if (!callingPackage.isNullOrEmpty()) {
            passwordCount = credentialsDataSource.getPasswordCount(callingPackage)
        }

        // Parse the request options into a PublicKeyCredentialRequestOptions object.
        if (!requestJson.isNullOrEmpty()) {
            val requestJsonObject = JSONObject(requestJson)
            val rp: JSONObject = requestJsonObject.getJSONObject("rp")
            val id: String = rp.getString("id")
            passkeyCount = credentialsDataSource.getPasskeysCount(id)
        }

        when (request) {
            // Handle Password credential
            is BeginCreatePasswordCredentialRequest -> {
                return handleCreateCredentialQuery(
                    passwordCount,
                    passkeyCount,
                    CREATE_PASSWORD_INTENT,
                )
            }

            // Handle Passkey credential
            is BeginCreatePublicKeyCredentialRequest -> {
                return handleCreateCredentialQuery(
                    passwordCount,
                    passkeyCount,
                    CREATE_PASSKEY_INTENT,
                )
            }
        }
        return null
    }

    /**
     * This method queries credentials from the storage used i.e database here, create password entries to populate.
     *
     * @param callingPackage The package name of the calling app.
     * @param option The BeginGetPasswordOption object containing the request parameters.
     * @param responseBuilder The Builder object used to build the BeginGetCredentialResponse.
     * @return True if credentials were found and added to the response builder, false otherwise.
     */
    private fun populatePasswordData(
        callingPackage: String,
        option: BeginGetPasswordOption,
        responseBuilder: Builder,
    ): Boolean {
        try {
            val credentials =
                credentialsDataSource.credentialsForSite(callingPackage) ?: return false
            val passwords = credentials.passwords
            val it = passwords.iterator()
            while (it.hasNext()) {
                val passwordItemCurrent = it.next()

                // Create Password entry
                val entry = PasswordCredentialEntry.Builder(
                    applicationContext,
                    passwordItemCurrent.username,
                    createNewPendingIntent(
                        passwordItemCurrent.username,
                        GET_PASSWORD_INTENT,
                    ),
                    option,
                )
                    .setDisplayName("display-${passwordItemCurrent.username}")
                    .setIcon(AppDependencies.providerIcon!!)
                    .setLastUsedTime(Instant.ofEpochMilli(passwordItemCurrent.lastUsedTimeMs))
                    .build()
                // Add the entry to the response builder.
                responseBuilder.addCredentialEntry(entry)
            }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    /**
     * This method queries credentials from your database, create passkey and password entries to populate.
     *
     * @param option   The BeginGetPublicKeyCredentialOption object containing the request parameters.
     * @param responseBuilder The Builder object used to build the BeginGetCredentialResponse.
     * @return True if credentials were found and added to the response builder, false otherwise.
     */
    private fun populatePasskeyData(
        option: BeginGetPublicKeyCredentialOption,
        responseBuilder: Builder,
    ): Boolean {
        try {
            // Parse the request options into a PublicKeyCredentialRequestOptions object.
            val request = PublicKeyCredentialRequestOptions(option.requestJson)

            // Get the credentials for the site specified in the request.
            val credentials = credentialsDataSource.credentialsForSite(request.rpId) ?: return false

            val passkeys = credentials.passkeys
            for (passkey in passkeys) {
                val data = Bundle()
                data.putString("requestJson", option.requestJson)
                data.putString("credId", passkey.credId)

                // Create a PendingIntent to launch the activity that will handle the passkey retrieval
                val pendingIntent = createNewPendingIntent(
                    "",
                    GET_PASSKEY_INTENT,
                    data,
                )

                // Create a PublicKeyCredentialEntry object to represent the passkey
                val entryBuilder = PublicKeyCredentialEntry.Builder(
                    applicationContext,
                    passkey.username,
                    pendingIntent,
                    option,
                )
                    .setDisplayName(passkey.displayName)
                    .setLastUsedTime(Instant.ofEpochMilli(passkey.lastUsedTimeMs))
                    .setIcon(AppDependencies.providerIcon!!)

                val entry = entryBuilder
                    .build()
                responseBuilder.addCredentialEntry(entry)
            }
        } catch (e: IOException) {
            return false
        }
        return true
    }

    /**
     * Creates a new PendingIntent for the given action and account ID.
     *
     * Any required data that the provider needs when the corresponding activity is invoked should be
     * set as an extra on the intent that's used to create your PendingIntent, such as an accountId in the creation flow.
     *
     * @param accountId The ID of the account to associate with the PendingIntent.
     * @param action The action to be performed when the PendingIntent is invoked.
     * @param extra Optional Bundle containing additional data to be passed to the activity.
     * @return A new PendingIntent.
     */
    private fun createNewPendingIntent(
        accountId: String,
        action: String,
        extra: Bundle? = null,
    ): PendingIntent {
        val intent = Intent(action).setPackage(applicationContext.packageName)
        if (extra != null) {
            intent.putExtra("VAULT_DATA", extra)
        }
        intent.putExtra(KEY_ACCOUNT_ID, accountId)
        return PendingIntent.getActivity(
            applicationContext,
            requestCode.incrementAndGet(),
            intent,
            (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    fun getRequestCounter(): AtomicInteger {
        return requestCode
    }

    /**
     * Adds a CreateEntry to the BeginCreateCredentialResponse.
     *
     * Each CreateEntry should correspond to an account where the credential can be saved,
     * and must have a PendingIntent set along with other required metadata.
     *
     * @param passwordCount The number of password credentials associated with the account.
     * @param passkeyCount The number of passkey credentials associated with the account.
     * @param intentType The type of intent to be used for the PendingIntent.
     * @return A BeginCreateCredentialResponse with the CreateEntry added.
     */
    private fun handleCreateCredentialQuery(
        passwordCount: Int,
        passkeyCount: Int,
        intentType: String,
    ): BeginCreateCredentialResponse {
        // Each CreateEntry should correspond to an account where the credential can be saved,
        // and must have a PendingIntent set along with other required metadata.
        return BeginCreateCredentialResponse.Builder()
            .addCreateEntry(
                createEntry(
                    intentType,
                    passwordCount,
                    passkeyCount,
                ),
            ).build()
    }

    /**
     * Creates a CreateEntry object for the user account based on their credential preferences.
     *
     * @param intentType The type of intent to be used for the PendingIntent.
     * @param passwordCount The number of password credentials associated with the account.
     * @param passkeyCount The number of passkey credentials associated with the account.
     * @return A CreateEntry object.
     */
    private fun createEntry(
        intentType: String,
        passwordCount: Int,
        passkeyCount: Int,
    ) = CreateEntry.Builder(
        USER_ACCOUNT,
        createNewPendingIntent(USER_ACCOUNT, intentType),
    ).setLastUsedTime(
        Instant.ofEpochMilli(
            sharedPreferences.getLong(
                KEY_ACCOUNT_LAST_USED_MS,
                0L,
            ),
        ),
    )
        .setPasswordCredentialCount(passwordCount)
        .setPublicKeyCredentialCount(passkeyCount)
        .setTotalCredentialCount(passwordCount + passkeyCount)
        .setDescription(
            CREDENTIAL_DESCRIPTION,
        )
        .build()

    companion object {
        private const val CREATE_PASSWORD_INTENT =
            "com.example.android.authentication.myvault.CREATE_PASSWORD"
        private const val CREATE_PASSKEY_INTENT =
            "com.example.android.authentication.myvault.CREATE_PASSKEY"
        private const val GET_PASSKEY_INTENT =
            "com.example.android.authentication.myvault.GET_PASSKEY"
        private const val GET_PASSWORD_INTENT =
            "com.example.android.authentication.myvault.GET_PASSWORD"
        const val KEY_ACCOUNT_LAST_USED_MS = "key_account_last_used_ms"
        const val KEY_ACCOUNT_ID = "key_account_id"
        const val USER_ACCOUNT = "user_account"
        const val CREDENTIAL_DESCRIPTION =
            "Your credential will be saved securely to the chosen account."
    }
}
