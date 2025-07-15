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

import android.content.Context
import com.authentication.shrinewear.authenticator.AuthenticationServer
import com.authentication.shrinewear.authenticator.CredentialManagerAuthenticator

/**
 * A simple, manual dependency injection container for application-wide singletons.
 *
 * This object serves as a central point to provide and access core services
 * and dependencies that are shared across the application.
 *
 * It requires a [Context] to be [provided][provide] during application startup
 * to initialize its dependencies.
 */
object Graph {
    /**
     * The authenticated instance of [CredentialManagerAuthenticator].
     * This property is initialized once via the [provide] method and
     * provides access to credential management and authentication services.
     *
     * It's a `lateinit var` because it's initialized after object creation (e.g., in `Application.onCreate()`).
     * The `private set` ensures that it can only be set once internally by the `Graph` object.
     */
    lateinit var credentialManagerAuthenticator: CredentialManagerAuthenticator
        private set
    lateinit var authenticationServer: AuthenticationServer
        private set

    /**
     * Stores the current authentication status code as an Android string resource ID.
     * This can be used to reflect the authentication state across different parts of the UI.
     *
     * Defaults to [R.string.credman_status_logged_out].
     */
    var authenticationStatusCode: Int = R.string.credman_status_logged_out

    /**
     * Provides and initializes the core dependencies for the application's [Graph].
     *
     * This method should be called once during the application's lifecycle (e.g., in the `Application.onCreate()` method)
     * to ensure all necessary services are set up.
     *
     * @param context The application [Context] required to initialize services like [CredentialManagerAuthenticator].
     */
    fun provide(context: Context) {
        credentialManagerAuthenticator = CredentialManagerAuthenticator(context)
        authenticationServer = AuthenticationServer()
    }
}
