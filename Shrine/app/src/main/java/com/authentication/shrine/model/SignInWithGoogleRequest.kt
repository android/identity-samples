package com.authentication.shrine.model

/**
 * Represents the request body for signing in with Google.
 * @param token the auth token retrieved from Credential Manager.
 * @param url a fixed url to send for Sign in with Google requests.
 */
data class SignInWithGoogleRequest(
    val token: String,
    val url: String = "https://accounts.google.com"
)