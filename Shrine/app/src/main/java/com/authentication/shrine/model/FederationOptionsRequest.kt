package com.authentication.shrine.model

/**
 * Represents the request body for getting federation options from the server.
 * @param urls a list of urls to send for federated requests.
 */
data class FederationOptionsRequest(
    val urls: List<String> = listOf("https://accounts.google.com")
)