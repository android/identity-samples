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
package com.example.android.authentication.myvault

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import androidx.room.Room
import com.example.android.authentication.myvault.data.CredentialsDataSource
import com.example.android.authentication.myvault.data.CredentialsRepository
import com.example.android.authentication.myvault.data.RPIconDataSource
import com.example.android.authentication.myvault.data.room.MyVaultDatabase

/**
 * This class is an application-level singleton object which is providing dependencies required for the app to function.
 * We recommend using dependency injection framework while working on production apps.
 */
object AppDependencies {
    lateinit var database: MyVaultDatabase
    lateinit var sharedPreferences: SharedPreferences
    lateinit var credentialsRepository: CredentialsRepository
    val credentialsDataSource by lazy {
        CredentialsDataSource(
            myVaultDao = database.myVaultDao(),
        )
    }

    var providerIcon: Icon? = null

    lateinit var RPIconDataSource: RPIconDataSource

    /**
     * Initializes the core components required for the application's data storage and icon handling.
     * This includes:
     * * **sharedPreference:** Creates a sharedpreference instance for storing application metadata.
     * * **database:** Creates a Room database instance for storing application data.
     * * **RPIconDataSource:** Initializes a data source for handling Relying Party icons (rpicons).
     * * **provider icon:** Sets a default icon to represent secure data providers.
     *
     * @param context The application context, used for accessing resources and file storage.
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            context.packageName,
            Context.MODE_PRIVATE,
        )

        database = Room.databaseBuilder(context, MyVaultDatabase::class.java, "my_vault.db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        RPIconDataSource = RPIconDataSource(context.applicationInfo.dataDir)
        providerIcon = Icon.createWithResource(context, R.drawable.android_secure)

        credentialsRepository =
            CredentialsRepository(
                sharedPreferences,
                credentialsDataSource,
                context,
            )
    }
}
