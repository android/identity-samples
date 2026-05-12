package com.example.jsbridgedigitalcredentials

import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class DigitalCredentialWebListener(
    private val coroutineScope: CoroutineScope,
    private val credentialManagerHandler: CredentialManagerHandler
) : WebViewCompat.WebMessageListener {

    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val messageData = message.data ?: return
        Log.d(TAG, "onPostMessage: $messageData")

        try {
            val jsonObj = JSONObject(messageData)
            val type = jsonObj.optString("type")
            val request = jsonObj.optString("request")

            if (type == "get") {
                coroutineScope.launch(Dispatchers.Main) {
                    val response = credentialManagerHandler.getDigitalCredential(request)
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                        replyProxy.postMessage(response)
                    }
                }
            } else {
                Log.w(TAG, "Unknown message type: $type")
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                    replyProxy.postMessage("{\"error\": \"Unknown message type: $type\"}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
            replyProxy.postMessage("{\"error\": \"Error parsing message: ${e.message}\"}")
        }
    }

    companion object {
        private const val TAG = "DigitalCredWebListener"
        const val INTERFACE_NAME = "DigitalCredentialBridge"

        fun register(
            webView: WebView,
            coroutineScope: CoroutineScope,
            credentialManagerHandler: CredentialManagerHandler
        ) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                val listener = DigitalCredentialWebListener(coroutineScope, credentialManagerHandler)
                WebViewCompat.addWebMessageListener(
                    webView,
                    INTERFACE_NAME,
                    setOf("*"),
                    listener
                )
                Log.d(TAG, "WebMessageListener registered")
            } else {
                Log.e(TAG, "WEB_MESSAGE_LISTENER not supported")
            }
        }
    }
}
