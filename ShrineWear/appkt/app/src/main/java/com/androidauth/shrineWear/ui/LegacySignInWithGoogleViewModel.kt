package com.androidauth.shrineWear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.androidauth.shrineWear.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInViewModel

val LegacySignInWithGoogleViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val application = this[APPLICATION_KEY]!!

        val gsiOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(BuildConfig.CLIENT_ID)
            .requestIdToken(BuildConfig.CLIENT_ID)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(application, gsiOptions)

        GoogleSignInViewModel(googleSignInClient, LegacySignInWithGoogleEventListener)
    }
}