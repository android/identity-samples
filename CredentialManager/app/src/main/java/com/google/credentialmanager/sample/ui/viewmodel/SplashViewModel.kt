/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.credentialmanager.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.credentialmanager.sample.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// This class is responsible for checking signin state and navigate to corresponding screen
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    suspend fun isSignedIn(): Boolean {
        return repository.isSignedIn()
    }

    suspend fun isSignedInThroughPasskeys(): Boolean {
        return repository.isSignedInThroughPasskeys()
    }
}


