package com.google.credentialmanager.sample

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

// Helper extension function to find the activity from LocalContext
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}