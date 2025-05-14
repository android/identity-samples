/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.androidauth.shrineWear

import android.util.Base64
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

const val SERVER_CLIENT_ID = BuildConfig.CLIENT_SECRET

data class CredentialData(
    val cookie: String?,
    val json: String?,
    val token: String?,
)

// Uses a real firebase server to obtain credentials created in the mobile Shrine application.
class AuthenticationServer {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val signedInState = MutableStateFlow(false)
    private val currentUser get() = firebaseAuth.currentUser

    init {
        firebaseAuth.addAuthStateListener {
            signedInState.value = it.currentUser != null
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun getPublicKeyServerParameters(): CredentialData {
        val request = mutableMapOf<String, String>()
        return getCredentialAssertionData("assertion_request", request)
    }

    fun createGetGoogleIdOption(autoSelect: Boolean): GetGoogleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SERVER_CLIENT_ID)
            .setAutoSelectEnabled(autoSelect)
            .build()

    suspend fun loginWithPassword(username: String, password: String): Boolean {
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(username, password).addOnSuccessListener {
                continuation.resume(true)
            }.addOnFailureListener {
                Log.e("TAG", "log in with saved password failed: $it")
                continuation.resume(false)
            }
        }
    }

    suspend fun loginWithGoogleToken(token: String): Boolean {
        return suspendCoroutine { continuation ->
            val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
            firebaseAuth.signInWithCredential(firebaseCredential).addOnSuccessListener {
                continuation.resume(true)
            }.addOnFailureListener {
                Log.e("TAG", "login with google id failed: $it")
                continuation.resume(false)
            }
        }
    }

    suspend fun loginWithPasskey(json: String, cookie: String): Boolean {
        val response = sendAuthenticationResponse(json, cookie)
        val token = response.token!!
        return loginWithPasskeyToken(token)
    }

    private suspend fun sendAuthenticationResponse(responseJson: String, cookie: String): CredentialData {
        val response = mutableMapOf<String, String>()
        response["json"] = b64Encode(responseJson.toByteArray())
        response["cookie"] = cookie
        return getCredentialAssertionData("assertion_response", response)
    }

    private suspend fun getCredentialAssertionData(
        requestType: String,
        request: Map<String, String>,
    ): CredentialData {
        return suspendCoroutine { continuation ->
            val data: MutableMap<String, Any> = HashMap()
            data[requestType] = request
            FirebaseFunctions.getInstance().getHttpsCallable("fcmtokens")
                .call(data).addOnSuccessListener {
                    val data = it.data as Map<*, *>
                    Log.i("TAG", "Server Replied")
                    val success = data["success"] as Boolean
                    if (success) {
                        var cookie: String? = null
                        var json: String? = null
                        var token: String? = null
                        if (data.containsKey("cookie")) {
                            cookie = data["cookie"] as String
                        }
                        if (data.containsKey("json")) {
                            json = String(b64Decode(data["json"] as String))
                        }
                        if (data.containsKey("token")) {
                            token = data["token"] as String
                        }
                        continuation.resume(CredentialData(cookie, json, token))
                    } else {
                        val error = data["error"] as String
                        Log.i("TAG", "Server Error: $error")
                        continuation.resumeWithException(IllegalArgumentException("error"))
                    }
                }.addOnFailureListener {
                    Log.w("TAG", "Server Failed: " + it.stackTraceToString())
                    continuation.resumeWithException(it)
                }
        }
    }

    private suspend fun loginWithPasskeyToken(token: String): Boolean {
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithCustomToken(token).addOnSuccessListener {
                continuation.resume(true)
            }.addOnFailureListener {
                Log.e("TAG", "login with token failed: $it")
                continuation.resume(false)
            }
        }
    }

    private fun b64Decode(str: String): ByteArray {
        return Base64.decode(str, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun b64Encode(str: ByteArray): String {
        return Base64.encodeToString(str, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
