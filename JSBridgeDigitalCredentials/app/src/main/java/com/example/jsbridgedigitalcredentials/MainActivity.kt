package com.example.jsbridgedigitalcredentials

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.jsbridgedigitalcredentials.ui.theme.JSBridgeDigitalCredentialsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JSBridgeDigitalCredentialsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DigitalCredentialWebView()
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DigitalCredentialWebView() {
    val coroutineScope = rememberCoroutineScope()
    
    AndroidView(
        factory = { context ->
            val handler = CredentialManagerHandler(context)
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("MainActivity", "Page finished loading: $url")
                    }
                }

                DigitalCredentialWebListener.register(this, coroutineScope, handler)

                loadUrl("file:///android_asset/index.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
