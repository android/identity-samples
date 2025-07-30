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
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class CredentialProviderService: CredentialProviderEventsService() {
    private val dataSource = AppDependencies.credentialsDataSource

    @SuppressLint("RestrictedApi")
    override fun onSignalCredentialStateRequest(
        request: ProviderSignalCredentialStateRequest,
        callback: ProviderSignalCredentialStateCallback,
    ) {
        when (request.callingRequest) {
            is SignalUnknownCredentialRequest -> {
                handleUnknownCredentialRequest(request.callingRequest.requestJson)
                showNotification(
                    getString(R.string.credential_deletion),
                    getString(R.string.unknown_signal_message)
                )
            }

            is SignalAllAcceptedCredentialIdsRequest -> {
                handleAcceptedCredentialsRequest(request.callingRequest.requestJson)
                showNotification(
                    getString(R.string.credentials_list_updation),
                    getString(R.string.all_accepted_signal_message)
                )
            }
            is SignalCurrentUserDetailsRequest -> {
                handleCurrentUserDetailRequest(request.callingRequest.requestJson)
                showNotification(
                    getString(R.string.user_details_updation),
                    getString(R.string.current_user_signal_message)
                )
            }
            else -> { }
        }

        callback.onSignalConsumed()
    }

    private fun handleUnknownCredentialRequest(requestJson: String) = runBlocking {
        val credentialId = JSONObject(requestJson).getString(CREDENTIAL_ID)
        dataSource.getPasskey(credentialId)?.let {
            dataSource.hidePasskey(it)
        }
    }

    private fun handleAcceptedCredentialsRequest(requestJson: String) = runBlocking {
        val request = JSONObject(requestJson)
        val userId = request.getString(USER_ID)
        val listCurrentPasskeysForUser = dataSource.getPasskeyForUser(userId) ?: emptyList()
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
    }

    private fun handleCurrentUserDetailRequest(requestJson: String) = runBlocking {
        val request = JSONObject(requestJson)
        val userId = request.getString(USER_ID)
        val updatedName = request.getString(NAME)
        val updatedDisplayName = request.getString(DISPLAY_NAME)
        val listPasskeys = dataSource.getPasskeyForUser(userId) ?: emptyList()
        // Update user details for each passkey
        for (key in listPasskeys) {
            val newPasskeyItem = key.copy(username = updatedName, displayName = updatedDisplayName)
            dataSource.updatePasskey(newPasskeyItem)
        }
    }
}
