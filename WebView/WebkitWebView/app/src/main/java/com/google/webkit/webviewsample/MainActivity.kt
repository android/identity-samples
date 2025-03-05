/*
 * Copyright 2025 The Android Open Source Project
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
package com.google.webkit.webviewsample

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
            val url = stringResource(R.string.server_url)
            WebViewSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    AndroidView(
                        factory = {
                            WebView(it).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = WebViewClientImpl()
                            }
                        },
                        update = { webView ->
                            run {
                                webView.loadUrl(url)
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
                                    WebSettingsCompat.setWebAuthenticationSupport(
                                        webView.settings,
                                        WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP,
                                    )
                                } else {
                                    // App does not support passkeys in WebView.
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
