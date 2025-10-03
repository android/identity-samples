package com.google.credentialmanager.sample

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError = _signInError.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun signIn(activity: Activity, context: Context) {
        _isLoading.value = true
        _signInError.value = null

        viewModelScope.launch {
            val data = getSavedCredentials(activity, context)

            if(data != null) {
                sendSignInResponseToServer()
                _navigationEvent.emit(NavigationEvent.NavigateToHome(signedInWithPasskeys = DataProvider.isSignedInThroughPasskeys()))
            }
        }
    }

    private suspend fun getSavedCredentials(activity: Activity, context: Context): String? {
        val getPublicKeyCredentialOption =
            GetPublicKeyCredentialOption(fetchAuthJsonFromServer(context), null)

        val getPasswordOption = GetPasswordOption()

        val credentialManager = CredentialManager.create(activity)
        val result = try {
            credentialManager.getCredential(
                activity,
                GetCredentialRequest(
                    listOf(
                        getPublicKeyCredentialOption,
                        getPasswordOption
                    )
                )
            )
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
            _signInError.value =
                "An error occurred while authenticating: " + e.message.toString()
            return null
        }

        if (result.credential is PublicKeyCredential) {
            val cred = result.credential as PublicKeyCredential
            DataProvider.setSignedInThroughPasskeys(true)
            return "Passkey: ${cred.authenticationResponseJson}"
        }
        if (result.credential is PasswordCredential) {
            val cred = result.credential as PasswordCredential
            DataProvider.setSignedInThroughPasskeys(false)
            return "Got Password - User:${cred.id} Password: ${cred.password}"
        }
        if (result.credential is CustomCredential) {
            //If you are also using any external sign-in libraries, parse them here with the utility functions provided.
        }

        return null
    }

    private fun fetchAuthJsonFromServer(context: Context): String {
        return context.assets.open("AuthFromServer").bufferedReader().use { it.readText() }
    }

    private fun sendSignInResponseToServer(): Boolean {
        return true
    }
}
