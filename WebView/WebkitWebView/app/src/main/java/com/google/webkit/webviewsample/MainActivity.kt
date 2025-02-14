package com.google.webkit.webviewsample

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.webkit.webviewsample.theme.WebViewSampleTheme


/**
 * Generates a WebView that uses the Webkit API to handle WebView passkey authentication requests.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val url = "https://alder-sunny-bicycle.glitch.me/"
            WebViewSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AndroidView(factory = {
                        WebView(it).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClientImpl()
                        }
                    },
                        update = { webView ->
                            run {
                                webView.loadUrl(url)
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
                                    Log.e("WebViewPasskeyDemo", "WebView supports passkeys.")
                                    WebSettingsCompat.setWebAuthenticationSupport(
                                        webView.settings,
                                        WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP,
                                    )
                                    Log.e(
                                        "WebViewPasskeyDemo",
                                        "getWebAuthenticationSupport result: " +
                                                WebSettingsCompat.getWebAuthenticationSupport(
                                                    webView.settings
                                                ),
                                    )
                                } else {
                                    Log.e(
                                        "WebViewPasskeyDemo",
                                        "WebView does not support passkeys."
                                    )
                                }
                            }
                        })
                }
            }
        }
    }
}