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
import android.content.Intent
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.Action
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.R
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/*
* This class extends CredentialProviderService() that provides abstract methods (to be implemented) used to save and retrieve credentials for a given user,
* upon the request of a client app that typically uses these credentials for sign-in flows.
*
* The credential retrieval and creation/saving is mediated by the Android System that
 * aggregates credentials from multiple credential provider services, and presents them to
 * the user in the form of a selector UI for credential selections/account selections/
 * confirmations etc.
 *
 */
class MyVaultService(private val credentialsRepository: CredentialsRepository = AppDependencies.credentialsRepository) :
    CredentialProviderService() {


    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.createCredential], to create/save a credential
     * with a credential provider installed on the device.
     *
     * @param [request] the [BeginCreateCredentialRequest] to handle
     * See [BeginCreateCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        // Handle the BeginCreateCredentialRequest by constructing a corresponding BeginCreateCredentialResponse and passing it through the callback.
        val response: BeginCreateCredentialResponse? =
            credentialsRepository.processCreateCredentialsRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(
                CreateCredentialUnknownException(),
            )
        }
    }

    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.getCredential], to get a credential
     * sourced from a credential provider installed on the device.
     *
     * @param [request] the [ProviderGetCredentialRequest] to handle
     * See [BeginGetCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        val callingPackage = request.callingAppInfo?.packageName
        if (callingPackage == null) {
            callback.onError(NoCredentialException())
        }

        // Turn this to true if you want your app to be locked during every launch
        val appLocked = false
        val responseBuilder = BeginGetCredentialResponse.Builder()

        // Note that if your credentials are locked, you can immediately set an AuthenticationAction on the response and invoke the callback.
        if (appLocked) {
            callback.onResult(
                responseBuilder.setAuthenticationActions(
                    listOf(
                        AuthenticationAction(
                            // Providers that require unlocking the credentials before returning any credentialEntries,
                            // must set up a pending intent that navigates the user to the app's unlock flow.
                            applicationContext.getString(R.string.app_name),
                            createPendingIntent(
                                credentialsRepository.getRequestCounter(),
                                UNLOCK_INTENT,
                            ),
                        ),
                    ),
                ).build(),
            )
            return
        }

        val hasCredentialsFound =
            credentialsRepository.processGetCredentialsRequest(request, responseBuilder)
        val hasActionsPopulated =
            populateActions(responseBuilder, credentialsRepository.getRequestCounter())

        if (hasCredentialsFound || hasActionsPopulated) {
            callback.onResult(
                responseBuilder.build(),
            )
            return
        }

        callback.onError(
            GetCredentialUnknownException(),
        )
    }

    /**
     * Called by the Android System in response to a client app calling
     * [androidx.credentials.CredentialManager.clearCredentialState]. A client app typically
     * calls this API on instances like sign-out when the intention is that the providers clear
     * any state that they may have maintained for the given user.
     *
     * You should invoked this api after your user signs out of your app to notify all credential
     * providers that any stored credential session for the given app should be cleared.
     *
     * @param request the request for the credential provider to handle
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // Delete any maintained state as appropriate.
        callback.onResult(null)
    }

    /**
     * Creates a PendingIntent that navigates the user to the app's open/unlock flow.
     *
     * @param counter An AtomicInteger used to generate a unique request code for the PendingIntent.
     * @param intentType The type of intent to create.
     * @return A PendingIntent that can be used to launch the app's open/unlock flow.
     */
    private fun createPendingIntent(counter: AtomicInteger, intentType: String): PendingIntent {
        val intent = Intent(intentType).setPackage(applicationContext.packageName)
        return PendingIntent.getActivity(
            applicationContext,
            counter.incrementAndGet(),
            intent,
            (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    /**
     * This method helps create an action builder for opening the app.
     *
     * @param responseBuilder The BeginGetCredentialResponse.Builder to add the action to.
     * @param counter An AtomicInteger used to generate a unique request code for the PendingIntent.
     * @return True if the action was added successfully, false otherwise.
     */
    private fun populateActions(
        responseBuilder: BeginGetCredentialResponse.Builder,
        counter: AtomicInteger,
    ): Boolean {
        try {
            responseBuilder.addAction(
                Action(
                    title = getString(
                        R.string.open,
                        applicationContext.getString(R.string.app_name),
                    ),
                    subtitle = getString(R.string.manage_credentials),
                    pendingIntent = createPendingIntent(counter, OPEN_APP_INTENT),
                ),
            )
        } catch (e: IOException) {
            return false
        }
        return true
    }

    companion object {
        private const val OPEN_APP_INTENT = "com.example.android.authentication.myvault.OPEN_APP"
        private const val UNLOCK_INTENT = "com.example.android.authentication.myvault.UNLOCK_APP"
    }
}
