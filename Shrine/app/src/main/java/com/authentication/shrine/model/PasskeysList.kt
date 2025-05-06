package com.authentication.shrine.model

import com.google.gson.annotations.SerializedName

data class PasskeysList(
    @SerializedName("rpId") var rpId: String,
    @SerializedName("userId") var userId: String,
    @SerializedName("credentials") val credentials: List<PasskeyCredential>
)

data class PasskeyCredential(
    @SerializedName("id") val id: String,
    @SerializedName("passkeyUserId") val passkeyUserId: String,
    @SerializedName("name") val name: String,
    @SerializedName("credentialType") val credentialType: String,
    @SerializedName("aaguid") val aaguid: String,
    @SerializedName("registeredAt") val registeredAt: Long,
    @SerializedName("providerIcon") val providerIcon: String
)
