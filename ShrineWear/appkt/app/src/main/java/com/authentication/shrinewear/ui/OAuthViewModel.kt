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
package com.authentication.shrinewear.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.authentication.shrinewear.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DeviceGrantState(
    val statusCode: Int = R.string.oauth_device_authorization_default,
    val resultMessage: String = "",
)

class OAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DeviceGrantState())
    val uiState: StateFlow<DeviceGrantState> = _uiState.asStateFlow()

    fun signInWithOauth() {
        viewModelScope.launch {
            // Step 1: Retrieve the verification URI
            showStatus(statusStringId = R.string.status_open_phone)
            val verificationInfo = retrieveVerificationInfo().getOrElse {
                showStatus(statusStringId = R.string.status_failed)
                return@launch
            }
            // Step 2: Show the pairing code & open the verification URI on the paired device
            showStatus(R.string.status_pairing_code, verificationInfo.userCode)
            fireRemoteIntent(verificationInfo.verificationUri)
            // Step 3: Poll the Auth server for the token
            val token = retrieveToken(verificationInfo.deviceCode, verificationInfo.interval)
            // Step 4: Use the token to make an authorized request
            val userName = retrieveUserProfile(token).getOrElse {
                showStatus(statusStringId = R.string.status_failed)
                return@launch
            }
            showStatus(R.string.status_authorized, userName)
        }
    }

    private fun showStatus(statusStringId: Int = 0, resultString: String = "") {
        _uiState.update {
            DeviceGrantState(statusStringId, resultString)
        }
    }

    // The response data when retrieving the verification
    data class VerificationInfo(
        val verificationUri: String,
        val userCode: String,
        val deviceCode: String,
        val interval: Int,
    )

    /**
     * Retrieve the information needed to verify the user. When performing this request, the server
     * generates a user & device code pair. The user code is shown to the user and opened on the
     * paired device. The device code is passed while polling the OAuth server.
     */
    private suspend fun retrieveVerificationInfo(): Result<VerificationInfo> {
        return try {
            Log.d(TAG, DEBUG_RETRIEVING_VERIFICATION_INF0)
            val responseJson = doPostRequest(
                url = "https://oauth2.googleapis.com/device/code",
                params = mapOf(
                    "client_id" to CLIENT_ID,
                    "scope" to "https://www.googleapis.com/auth/userinfo.profile",
                ),
            )
            Result.success(
                VerificationInfo(
                    verificationUri = responseJson.getString("verification_url"),
                    userCode = responseJson.getString("user_code"),
                    deviceCode = responseJson.getString("device_code"),
                    interval = responseJson.getInt("interval"),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Opens the verification URL on the paired device.
     *
     * When the user has the corresponding app installed on their paired Android device, the Data
     * Layer can be used instead, see https://developer.android.com/training/wearables/data-layer.
     *
     * When the user has the corresponding app installed on their paired iOS device, it should
     * use [Universal Links](https://developer.apple.com/ios/universal-links/) to intercept the
     * intent.
     */
    private fun fireRemoteIntent(verificationUri: String) {
        RemoteActivityHelper(getApplication()).startRemoteActivity(
            Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse(verificationUri)
            },
            null,
        )
    }

    /**
     * Poll the Auth server for the token. This will only return when the user has finished their
     * authorization flow on the paired device.
     *
     * For this sample the various exceptions aren't handled.
     */
    private tailrec suspend fun retrieveToken(deviceCode: String, interval: Int): String {
        Log.d(TAG, DEBUG_POLLING_FOR_TOKEN)
        return fetchToken(deviceCode).getOrElse {
            Log.d(TAG, DEBUG_NO_TOKEN_YET)
            delay(interval * 1000L)
            return retrieveToken(deviceCode, interval)
        }
    }

    private suspend fun fetchToken(deviceCode: String): Result<String> {
        return try {
            val responseJson = doPostRequest(
                url = "https://oauth2.googleapis.com/token",
                params = mapOf(
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET,
                    "device_code" to deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                ),
            )

            Result.success(responseJson.getString("access_token"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Using the access token, make an authorized request to the Auth server to retrieve the user's
     * profile.
     */
    private suspend fun retrieveUserProfile(token: String): Result<String> {
        return try {
            val responseJson = doGetRequest(
                url = "https://www.googleapis.com/oauth2/v2/userinfo",
                requestHeaders = mapOf(
                    "Authorization" to "Bearer $token",
                ),
            )
            Result.success(responseJson.getString("name"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Simple implementation of a POST request. Normally you'd use a library to do these requests.
     */
    private suspend fun doPostRequest(
        url: String,
        params: Map<String, String>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject {
        return withContext(dispatcher) {
            val conn = (URL(url).openConnection() as HttpURLConnection)
            val postData = StringBuilder()
            for ((key, value) in params) {
                if (postData.isNotEmpty()) postData.append('&')
                postData.append(URLEncoder.encode(key, "UTF-8"))
                postData.append('=')
                postData.append(URLEncoder.encode(value, "UTF-8"))
            }
            val postDataBytes = postData.toString().toByteArray(charset("UTF-8"))

            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Content-Length", postDataBytes.size.toString())
                doOutput = true
                outputStream.write(postDataBytes)
            }

            val inputReader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = inputReader.readText()

            Log.d(TAG, DEBUG_POST_REQUEST_UTIL_RESPONSE.format(response))

            JSONObject(response)
        }
    }

    /**
     * Simple implementation of a GET request. Normally you'd use a library to do these requests.
     */
    private suspend fun doGetRequest(
        url: String,
        requestHeaders: Map<String, String>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): JSONObject {
        return withContext(dispatcher) {
            val conn = (URL(url).openConnection() as HttpURLConnection)
            requestHeaders.onEach { (key, value) ->
                conn.setRequestProperty(key, value)
            }
            val inputReader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = inputReader.readText()

            Log.d(TAG, DEBUG_REQUEST_UTIL_RESPONSE.format(response))

            JSONObject(response)
        }
    }

    companion object {
        // Todo(reader): Add your client secret here and server id here.
        private const val CLIENT_ID = ""
        private const val CLIENT_SECRET = ""

        // Logging constants
        private const val TAG = "OAuthViewModel"
        private const val DEBUG_NO_TOKEN_YET = "No token yet. Waiting..."
        private const val DEBUG_POLLING_FOR_TOKEN = "Polling for token..."
        private const val DEBUG_POST_REQUEST_UTIL_RESPONSE = "PostRequestUtil Response: %s"
        private const val DEBUG_REQUEST_UTIL_RESPONSE = "RequestUtil Response: %s"
        private const val DEBUG_RETRIEVING_VERIFICATION_INF0 = "Retrieving verification info..."
    }
}
