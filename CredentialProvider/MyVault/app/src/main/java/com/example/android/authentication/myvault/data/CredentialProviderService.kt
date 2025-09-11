package com.example.android.authentication.myvault.data

import android.annotation.SuppressLint
import android.util.Log
import androidx.credentials.SignalAllAcceptedCredentialIdsRequest
import androidx.credentials.SignalCurrentUserDetailsRequest
import androidx.credentials.SignalUnknownCredentialRequest
import androidx.credentials.providerevents.service.CredentialProviderEventsService
import androidx.credentials.providerevents.signal.ProviderSignalCredentialStateCallback
import androidx.credentials.providerevents.signal.ProviderSignalCredentialStateRequest
import com.example.android.authentication.myvault.ACCEPTED_CREDENTIAL_IDS
import com.example.android.authentication.myvault.AppDependencies
import com.example.android.authentication.myvault.CREDENTIAL_ID
import com.example.android.authentication.myvault.DISPLAY_NAME
import com.example.android.authentication.myvault.NAME
import com.example.android.authentication.myvault.R
import com.example.android.authentication.myvault.USER_ID
import com.example.android.authentication.myvault.showNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * A service that listens to credential provider events triggered by the relying parties
 *
 * This service is responsible for handling signals related to credential state changes in the RPs,
 * such as when a credential is no longer valid, when a list of accepted credentials is accepted,
 * or when current user details change for credentials
 */
class CredentialProviderService: CredentialProviderEventsService() {
    private val dataSource = AppDependencies.credentialsDataSource
    private val coroutineScope = AppDependencies.coroutineScope

    /**
     * Called when the system or another credential provider signals a change in credential state.
     *
     * This method inspects the type of [ProviderSignalCredentialStateRequest] and delegates
     * to the appropriate handler function to update the local data store and show a notification.
     * After processing the signal, {@link ProviderSignalCredentialStateCallback#onSignalConsumed()}
     * is called to acknowledge receipt of the signal.
     *
     * The {@link SuppressLint("RestrictedApi")} annotation is used because this method
     * interacts with APIs from the {@code androidx.credentials} library that might be
     * marked as restricted for extension by library developers.
     *
     * @param request The request containing details about the credential state signal.
     * @param callback The callback to be invoked after the signal has been processed.
     */
    @SuppressLint("RestrictedApi")
    override fun onSignalCredentialStateRequest(
        request: ProviderSignalCredentialStateRequest,
        callback: ProviderSignalCredentialStateCallback,
    ) {
        when (request.callingRequest) {
            is SignalUnknownCredentialRequest -> {
                updateDataOnSignalAndShowNotification(
                    handleRequest = ::handleUnknownCredentialRequest,
                    requestJson = request.callingRequest.requestJson,
                    notificationTitle = getString(R.string.credential_deletion),
                    notificationContent = getString(R.string.unknown_signal_message)
                )
            }

            is SignalAllAcceptedCredentialIdsRequest -> {
                updateDataOnSignalAndShowNotification(
                    handleRequest = ::handleAcceptedCredentialsRequest,
                    requestJson = request.callingRequest.requestJson,
                    notificationTitle = getString(R.string.credentials_list_updation),
                    notificationContent = getString(R.string.all_accepted_signal_message)
                )
            }

            is SignalCurrentUserDetailsRequest -> {
                updateDataOnSignalAndShowNotification(
                    handleRequest = ::handleCurrentUserDetailRequest,
                    requestJson = request.callingRequest.requestJson,
                    notificationTitle = getString(R.string.user_details_updation),
                    notificationContent = getString(R.string.current_user_signal_message)
                )
            }

            else -> { }
        }

        callback.onSignalConsumed()
    }

    /**
     * A helper function to asynchronously handle a credential state update request,
     * update the data source, and then show a system notification on the main thread.
     *
     * @param handleRequest A suspend function that takes the request JSON string and processes it.
     *                      This function is responsible for interacting with the data source.
     * @param requestJson The JSON string payload from the original credential signal request.
     * @param notificationTitle The title to be used for the system notification.
     * @param notificationContent The content text for the system notification.
     */
    private fun updateDataOnSignalAndShowNotification(
        handleRequest: suspend (String) -> Boolean,
        requestJson: String,
        notificationTitle: String,
        notificationContent: String,
    ) {
        coroutineScope.launch {
            val success = handleRequest(requestJson)
            withContext(Dispatchers.Main) {
                if (success) {
                    showNotification(
                        title = notificationTitle,
                        content = notificationContent,
                    )
                }
            }
        }
    }

    /**
     * Handles a [SignalUnknownCredentialRequest] by parsing the credential ID
     * from the request JSON and attempting to hide the corresponding passkey in the data source.
     *
     * "Hiding" a passkey typically means marking it as inactive or not to be suggested
     * for autofill, often because the system has indicated it's no longer valid
     * (e.g., deleted from the authenticator).
     *
     * @param requestJson The JSON string payload from the [SignalUnknownCredentialRequest].
     *                   Expected to contain a {@code CREDENTIAL_ID}.
     */
    private suspend fun handleUnknownCredentialRequest(requestJson: String): Boolean {
        try {
            val credentialId = JSONObject(requestJson).getString(CREDENTIAL_ID)
            dataSource.getPasskey(credentialId)?.let {
                // Currently hiding the passkey on UnknownSignal for testing purpose
                // If the business logc requires deletion, please add deletion code instead
                dataSource.hidePasskey(it)
            }
            return true
        } catch (e: Exception) {
            Log.e(getString(R.string.failed_to_handle_unknowncredentialrequest),  e.toString())
            return false
        }
    }

    /**
     * Handles a {@link SignalAllAcceptedCredentialIdsRequest} by synchronizing the visibility
     * state of passkeys for a specific user.
     *
     * It retrieves all current passkeys for the user from the data source. Then, it compares
     * this list against the list of accepted credential IDs provided in the signal.
     * Passkeys whose IDs are in the accepted list are unhidden (made active).
     * Passkeys whose IDs are not in the accepted list are hidden (made inactive).
     *
     * This is useful for scenarios where the system provides an authoritative list of
     * credentials that are currently valid or preferred for a user.
     *
     * @param requestJson The JSON string payload from the {@link SignalAllAcceptedCredentialIdsRequest}.
     *                   Expected to contain a {@code USER_ID} and {@code ACCEPTED_CREDENTIAL_IDS}
     *                   (which can be a string or a JSON array of strings).
     */
    private suspend fun handleAcceptedCredentialsRequest(requestJson: String): Boolean {
        try {
            val request = JSONObject(requestJson)
            val userId = request.getString(USER_ID)
            val listCurrentPasskeysForUser = dataSource.getAllPasskeysForUser(userId) ?: emptyList()
            val listAllAcceptedCredIds = mutableListOf<String>()
            when (val value = request.get(ACCEPTED_CREDENTIAL_IDS)) {
                is String -> listAllAcceptedCredIds.add(value)
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val item = value.get(i)
                        if (item is String) {
                            listAllAcceptedCredIds.add(item)
                        }
                    }
                }

                else -> { /*do nothing*/ }
            }

            for (key in listCurrentPasskeysForUser) {
                if (listAllAcceptedCredIds.contains(key.credId)) {
                    dataSource.unhidePasskey(key)
                } else {
                    dataSource.hidePasskey(key)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(getString(R.string.failed_to_handle_acceptedcredentialsrequest), e.toString())
            return false
        }
    }

    /**
     * Handles a {@link SignalCurrentUserDetailsRequest} by updating the username and display name
     * for all passkeys associated with a given user ID.
     *
     * This is useful when the user's profile information (like name or display name)
     * changes elsewhere, and the credential provider needs to reflect these changes
     * in its stored passkey data.
     *
     * @param requestJson The JSON string payload from the {@link SignalCurrentUserDetailsRequest}.
     *                   Expected to contain {@code USER_ID}, {@code NAME}, and {@code DISPLAY_NAME}.
     */
    private suspend fun handleCurrentUserDetailRequest(requestJson: String): Boolean {
        try {
            val request = JSONObject(requestJson)
            val userId = request.getString(USER_ID)
            val updatedName = request.getString(NAME)
            val updatedDisplayName = request.getString(DISPLAY_NAME)
            val listPasskeys = dataSource.getAllPasskeysForUser(userId) ?: emptyList()
            // Update user details for each passkey
            for (key in listPasskeys) {
                val newPasskeyItem =
                    key.copy(username = updatedName, displayName = updatedDisplayName)
                dataSource.updatePasskey(newPasskeyItem)
            }
            return true
        } catch (e: Exception) {
            Log.e(getString(R.string.failed_to_handle_currentuserdetailrequest), e.toString())
            return false
        }
    }
}
