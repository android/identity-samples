package com.google.credentialmanager.sample

import android.app.Activity
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialManager

suspend fun createCredential(activity: Activity, request: CreateCredentialRequest): CreateCredentialResponse {
    val credentialManager = CredentialManager.create(activity)
    return credentialManager.createCredential(activity, request)
}