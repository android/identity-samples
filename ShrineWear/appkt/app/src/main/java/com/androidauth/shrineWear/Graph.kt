package com.androidauth.shrineWear

import android.content.Context

object Graph {
  lateinit var credentialManagerAuthenticator: CredentialManagerAuthenticator
    private set
  var authenticationStatusCode: Int = R.string.credman_status_logged_out

  fun provide(context: Context) {
    credentialManagerAuthenticator = CredentialManagerAuthenticator(context)
  }
}