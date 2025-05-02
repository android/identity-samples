package com.androidauth.shrineWear.ui

import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidauth.shrineWear.CredentialType
import com.androidauth.shrineWear.Graph
import com.androidauth.shrineWear.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val statusCode: Int = R.string.credman_status_logged_out,
    val inProgress: Boolean = false,
    // TODO(johnzoeller): Move to datastore preferences.
    val credentialTypes: List<CredentialType> = CredentialType.entries
)

class CredentialManagerViewModel : ViewModel() {
    private val credManAuthenticator = Graph.credentialManagerAuthenticator
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(
        context: Context,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inProgress = true)

            try {
                if (credManAuthenticator.signInWithCredentialManager(
                        context, uiState.value.credentialTypes
                    )
                ) {
                    Graph.authenticationStatusCode = R.string.credman_status_authorized
                } else {
                    Graph.authenticationStatusCode = R.string.status_failed
                }
            } catch (e: GetCredentialCancellationException) {
                // TODO(johnzoeller): Implement GetCredentialException subtypes
                Log.i(
                    TAG, "Credential Manager dismissed by user, falling back to legacy " +
                            "authentication. Exception details: ${e.errorMessage}"
                )
                Graph.authenticationStatusCode = R.string.credman_status_dismissed
            } catch (e: NoCredentialException) {
                Log.e(TAG, "Missing credentials. Verify device SDK>35.")
                Graph.authenticationStatusCode = R.string.credman_status_no_credentials
            } catch (e: Exception) {
                // This is happening when I attempt passkey, known issue on internet, passkey
                // cannot verify itself-- makes sense, I created the passkey on another app.
                Log.e(TAG, "Authentication exception: ${e.message}.")
                Graph.authenticationStatusCode = R.string.credman_status_unknown
            } finally {
                _uiState.value = _uiState.value.copy(
                    inProgress = false,
                    statusCode = Graph.authenticationStatusCode
                )
            }
        }
    }

    companion object {
        private const val TAG = "CredentialManagerLoginViewModel"
    }
}
