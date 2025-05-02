package com.androidauth.shrineWear

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.MaterialTheme
import com.androidauth.shrineWear.ui.ShrineApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(
            "ValidateCredentialManager",
            "Build.VERSION.SDK_INT: ${Build.VERSION.SDK_INT}, has credential manager:" +
                    "${getSystemService("credential") != null}"
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShrineApp()
            }
        }
    }
}

