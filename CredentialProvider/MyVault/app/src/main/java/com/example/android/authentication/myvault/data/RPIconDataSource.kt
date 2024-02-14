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
package com.example.android.authentication.myvault.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * This class is responsible for providing the icons of the corresponding apps (who are saving credentials through MyVault).
 *
 * The RPIconDataSource class provides methods to get, save, and read rpicons from disk and the network.
 */
class RPIconDataSource(private var dataDir: String) {

    /**
     * A mutable map to store the icons.
     */
    private val icons: MutableMap<String, Bitmap> = mutableMapOf()

    /**
     * Gets the file for the given URL.
     *
     * @param url The URL to get the file for.
     * @return The File object for the given URL.
     */
    private fun getFileForUrl(url: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(url.toByteArray())
        val hashName = hash.joinToString(separator = "") { b -> "%02x".format(b) }
        return File("$dataDir/rpicons", "$hashName.png")
    }

    /**
     * Saves the icon to disk.
     *
     * @param url The URL of the icon.
     * @param icon The Bitmap object of the icon.
     */
    private suspend fun saveToDisk(url: String, icon: Bitmap) {
        return withContext(Dispatchers.IO) {
            val f = getFileForUrl(url)
            f.parentFile?.mkdirs()
            f.createNewFile()
            f.outputStream().use {
                icon.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
        }
    }

    /**
     * Gets the icon from the network.
     *
     * @param url The URL of the icon.
     * @return The Bitmap object of the icon, or null if there was an error.
     */
    private suspend fun getIconFromNetwork(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            var icon: Bitmap? = null
            try {
                val connection =
                    URL("https://$url/rpicon.ico").openConnection() as HttpURLConnection
                icon = BitmapFactory.decodeStream(connection.inputStream)
                if (icon != null) {
                    saveToDisk(url, icon)
                }
            } catch (e: Exception) {
                // Handle the exception
            }
            icon
        }
    }

    /**
     * Reads the icon from disk.
     *
     * @param url The URL of the icon.
     * @return The Bitmap object of the icon, or null if there was an error.
     */
    private suspend fun readFromDisk(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            val f = getFileForUrl(url)
            var icon: Bitmap? = null
            try {
                icon = BitmapFactory.decodeStream(f.inputStream())
            } catch (e: FileNotFoundException) {
                // File not found, handle error
            }
            icon
        }
    }

    /**
     * Gets the icon for the provided domain URL.
     *
     * @param url The domain URL to get the icon for.
     * @return The Bitmap object of the icon, or null if there was an error.
     */
    suspend fun getIcon(url: String): Bitmap? {
        if (!icons.containsKey(url)) {
            val icon = readFromDisk(url) ?: getIconFromNetwork(url)
            if (icon != null) {
                icons[url] = icon
            }
            return icons[url]
        }
        return null
    }
}
