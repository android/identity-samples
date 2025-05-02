package com.androidauth.shrineWear.ui

import android.util.Log
import com.androidauth.shrineWear.Graph
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener

object LegacySignInWithGoogleEventListener : GoogleSignInEventListener {
    private val TAG = this::class.java.simpleName

    override suspend fun onSignedIn(account: GoogleSignInAccount) {
        Log.i(TAG, "Legacy Google Account received: ${account.displayName}. Registering to " +
                    "application credential repository")
        account.idToken?.takeIf { it.isNotEmpty() }?.let { token ->
            Graph.credentialManagerAuthenticator.registerAuthenticatedGoogleToken(token)
        } ?: run {
            Log.e(TAG, "Signed in, but failed to register Legacy Google sign in account to " +
                    "application repository due to missing Google Sign in idToken. " +
                    "Verify OAuthClient type is 'web' and that " +
                    "GoogleSignInOptionsBuilder.requestIdToken is passed the correct client id.")
        }
    }
}