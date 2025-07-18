package com.authentication.shrinewear.extensions

import android.util.JsonReader
import android.util.JsonToken.STRING
import android.util.Log
import com.authentication.shrinewear.network.AuthNetworkClient.Companion.SESSION_ID_KEY
import com.authentication.shrinewear.network.NetworkException
import com.authentication.shrinewear.network.NetworkResult
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.io.StringReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "OkHttpExtension"

suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                Log.w(TAG, "Exception thrown while trying to cancel a Call", ex)
            }
        }
    }
}

/**
 * Extension function for [Response] to convert it to an [NetworkResult].
 *
 * @param errorMessage The error message to use if the response is not successful.
 * @param data A lambda expression that extracts the data from the response.
 * @return An [NetworkResult] containing the data or an error.
 */
fun <T> Response.result(
    errorMessage: String,
    data: Response.() -> T
): NetworkResult<T> {
    if (!isSuccessful) {
        if (code == 401) { // Unauthorized
            return NetworkResult.SignedOutFromServer
        }
        // All other errors throw an exception.
        throwResponseError(this, errorMessage)
    }
    val cookie = headers("set-cookie").find { it.startsWith(SESSION_ID_KEY) }
    val sessionId = if (cookie != null) parseSessionId(cookie) else null
    return NetworkResult.Success(sessionId, data())
}

/**
 * Throws an [NetworkException] based on the given response and message.
 *
 * @param response The response object.
 * @param message The error message.
 */
private fun throwResponseError(response: Response, message: String): Nothing {
    val responseBody = response.body
    if (responseBody != null) {
        throw NetworkException("$message; ${parseError(responseBody)}")
    } else {
        throw NetworkException(message)
    }
}

/**
 * Parses the error message from the given response body.
 *
 * @param body The response body.
 * @return The error message, or an empty string if it cannot be parsed.
 */
private fun parseError(body: ResponseBody): String {
    val errorString = body.string()
    try {
        JsonReader(StringReader(errorString)).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "error") {
                    val token = reader.peek()
                    if (token == STRING) {
                        return reader.nextString()
                    }
                    return "Unknown"
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Cannot parse the error: $errorString", e)
        // Don't throw; this method is called during throwing.
    }
    return ""
}

/**
 * Parses the session ID from the given cookie.
 *
 * @param cookie The cookie string.
 * @return The session ID.
 */
private fun parseSessionId(cookie: String): String {
    val start = cookie.indexOf(SESSION_ID_KEY)
    if (start < 0) {
        throw NetworkException("Cannot find $SESSION_ID_KEY")
    }
    val semicolon = cookie.indexOf(";", start + SESSION_ID_KEY.length)
    val end = if (semicolon < 0) cookie.length else semicolon
    return cookie.substring(start + SESSION_ID_KEY.length, end)
}