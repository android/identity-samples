/*
 * Copyright 2025 Google LLC
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

package com.google.credentialmanager.sample

/**
 * A sealed class that represents navigation events in the application.
 */
sealed class NavigationEvent {
    /**
     * A navigation event that navigates to the home screen.
     *
     * @param signedInWithPasskeys True if the user signed in with passkeys, false otherwise.
     */
    data class NavigateToHome(val signedInWithPasskeys: Boolean) : NavigationEvent()
}
