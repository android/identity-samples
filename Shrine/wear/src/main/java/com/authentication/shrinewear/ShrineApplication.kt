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
package com.authentication.shrinewear

import android.app.Application

/**
 * Custom [Application] class for the Shrine Wear OS application.
 *
 * This class is responsible for performing application-level initialization tasks,
 * such as setting up the dependency graph.
 */
class ShrineApplication : Application() {
    /**
     * Called when the application is first created.
     *
     * This is the primary entry point for application-level setup.
     * It calls [Graph.provide] to initialize the application's core dependencies
     * and services, making them available throughout the app.
     */
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}
