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
package com.authentication.shrine

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.authentication.shrine.api.AddHeaderInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The main application class for the Shrine app.
 */
@HiltAndroidApp
class ShrineApplication : Application()

/**
 * A Dagger Hilt module that provides dependencies for the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Creates and provides an OkHttpClient instance with interceptors and timeouts.
     *
     * @return The OkHttpClient instance.
     */
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND})"
        return OkHttpClient.Builder()
            .addInterceptor(AddHeaderInterceptor(userAgent))
            .addInterceptor(HttpLoggingInterceptor { message ->
                println("LOG-APP: $message")
            }.apply {
                level= HttpLoggingInterceptor.Level.BODY
            })
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides a singleton instance of the CoroutineScope.
     *
     * @return The CoroutineScope instance.
     */
    @Singleton
    @Provides
    fun provideAppCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    /**
     * Provides a DataStore instance with the file name "auth".
     *
     * @param application The application context.
     * @return The DataStore instance.
     */
    @Singleton
    @Provides
    fun provideDataStore(application: Application): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            application.preferencesDataStoreFile("auth")
        }
    }

    @Singleton
    @Provides
    fun providesCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    @Singleton
    @Provides
    fun providesCredentialManagerUtils(
        credentialManager: CredentialManager,
    ): CredentialManagerUtils {
        return CredentialManagerUtils(credentialManager)
    }
}
