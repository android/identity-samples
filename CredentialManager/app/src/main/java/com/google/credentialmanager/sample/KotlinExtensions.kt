package com.google.credentialmanager.sample

import android.content.Context
import androidx.appcompat.app.AlertDialog


    fun Context.readFromAsset(fileName: String): String {
        var data = ""
        this.assets.open(fileName).bufferedReader().use {
            data = it.readText()
        }
        return data
    }

    fun Context.showErrorAlert(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("An error occurred")
            .setMessage(msg)
            .setNegativeButton("Ok", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

