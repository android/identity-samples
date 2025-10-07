package com.google.credentialmanager.sample

import android.content.Context

class JsonProvider(private val context: Context) {
    fun fetchRegistrationJson(): String {
        return context.assets.open("RegFromServer").bufferedReader().use { it.readText() }
    }

    fun fetchAuthJson(): String {
        return context.assets.open("AuthFromServer").bufferedReader().use { it.readText() }
    }
}
