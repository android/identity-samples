/*
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.credentialmanager.sample.repository

/**
 * Represents the sign in/out state of the user. Used to navigate between screens in the app.
 */
sealed class SignInState {

    /**
     * The user is signed out.
     */
    object SignedOut : SignInState()

    /**
     * The user is signing in. The user has entered the username and is ready to sign in with
     * password or FIDO2.
     */
    data class SigningIn(
        val username: String
    ) : SignInState()

    /**
     * The user sign-in failed.
     */
    data class SignInError(
        val error: String
    ) : SignInState()

    /**
     * The user is signed in.
     */
    data class SignedIn(
        val username: String
    ) : SignInState()
}
